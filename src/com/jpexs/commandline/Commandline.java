package com.jpexs.commandline;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Commandline arguments parser
 *
 * @author JPEXS
 */
public class Commandline {

    private final Map<String, String> options = new HashMap<>();

    private final Map<String, List<Object>> optionValues = new HashMap<>();
    private final Map<String, String> optionDescriptions = new HashMap<>();
    private String finalArgType = "";
    private final List<Object> finalValues = new ArrayList<>();

    private final Map<String, OptionActionListener> actions = new HashMap<>();

    private final List<String[]> groups = new ArrayList<>();

    private final List<String> optionsOrder = new ArrayList<>();
    private final List<String> requiredOptions = new ArrayList<>();

    /**
     * The option is not requires.
     */
    public static final int REQUIRED_NONE = 0;
    /**
     * The option is required if there is no standalone option.
     */
    public static final int REQUIRED_YES = 1;
    /**
     * Standalone option. No other options are then required
     */
    public static final int REQUIRED_ALONE = 2;

    private final List<String> standaloneOptions = new ArrayList<>();

    private String appCommandline = "java -jar app.jar";

    public Commandline() {
        initHelp(); //add --help command for usage
    }

    private void initHelp() {
        addOption("help", "Prints usage", "", (String option, Object[] values, String[] valuesStr) -> {
            printUsage(System.err);
        }, REQUIRED_ALONE);
    }

    /**
     * Set Application executable name to print on usages
     *
     * @param appCommandline String
     */
    public void setAppCommandline(String appCommandline) {
        this.appCommandline = appCommandline;
    }

    private String[] objArrToStrArr(Object[] values) {
        if (values == null) {
            return null;
        }
        String ret[] = new String[values.length];
        for (int i = 0; i < values.length; i++) {
            ret[i] = "" + values[i];
        }
        return ret;
    }

    /**
     * Get values of option arguments as strings
     *
     * @param option Option name without dash prefix
     * @return Array of strings
     */
    public String[] getOptionStrValues(String option) {
        return objArrToStrArr(getOptionValues(option));
    }

    /**
     * Get single value of option arguments as string, or null if it does not
     * exist
     *
     * @param option Option name without dash prefix
     * @return String value or null
     */
    public String getOptionStrValue(String option) {
        return getOptionStrValue(option, null);
    }

    /**
     * Get single value of option arguments as string, or defaultValue if it
     * does not exist
     *
     * @param option Option name without dash prefix
     * @param defaultValue Default value
     * @return String value or defaultValue
     */
    public String getOptionStrValue(String option, String defaultValue) {
        return "" + getOptionValue(option, defaultValue);
    }

    /**
     * Check if option is set
     *
     * @param option Option name without dash prefix
     * @return True if it is set
     */
    public boolean isOptionOn(String option) {
        return optionValues.containsKey(option);
    }

    /**
     * Get single value of option arguments as object, or null if it does not
     * exist
     *
     * @param option Option name without dash prefix
     * @return Object value or null
     */
    public Object getOptionValue(String option) {
        return getOptionValue(option, null);
    }

    /**
     * Get single value of option arguments as string, or defaultValue if it
     * does not exist
     *
     * @param option Option name without dash prefix
     * @param defaultValue Default value
     * @return Object value or defaultValue
     */
    public Object getOptionValue(String option, Object defaultValue) {
        if (!optionValues.containsKey(option)) {
            return defaultValue;
        }
        if (optionValues.get(option).isEmpty()) {
            return defaultValue;
        }
        return optionValues.get(option).get(0);
    }

    protected static String formatOptionName(String option) {
        if (option.length() == 1) {
            return "-" + option;
        }
        return "--" + option;
    }

    /**
     * Get values of option arguments
     *
     * @param option Option name without dash prefix
     * @return Array of object values
     */
    public Object[] getOptionValues(String option) {
        if (!options.containsKey(option)) {
            throw new IllegalArgumentException("No option " + formatOptionName(option) + " defined");
        }
        List<Object> ret = optionValues.get(option);

        return ret == null ? null : ret.toArray(new Object[ret.size()]);
    }

    /**
     * Gets arguments as array of Strings
     *
     * @return Array of strings
     */
    public String[] getArgumentStrValues() {
        return objArrToStrArr(getArgumentValues());
    }

    /**
     * Gets arguments as array of Objects
     *
     * @return Array of objects
     */
    public Object[] getArgumentValues() {
        return finalValues.toArray(new Object[finalValues.size()]);
    }

    /**
     * Do not allow mix these options together
     *
     * @param grouped List of param names without dash prefix
     */
    public void disableOptionsTogether(String... grouped) {
        groups.add(grouped);
    }

    protected void checkArgType(String argTypes) {
        char prevType = '-';
        for (int i = 0; i < argTypes.length(); i++) {

            char c = argTypes.charAt(i);
            switch (c) {
                case ' ':
                    break;
                case 's':
                case 'i':
                    prevType = c;
                    break;
                case '<':
                    i = argTypes.indexOf('>', i + 1);
                    if (i == -1) {
                        throw new IllegalArgumentException("Invalid arg type, unclosed parent");
                    }
                    break;
                case '+':
                case '*':
                    if (prevType == '-') {
                        throw new IllegalArgumentException("Invalid arg type, '" + c + "' modifier before type");
                    }
                    break;
            }
        }
    }

    /**
     * Add supported option which is not required and has no bound action and no
     * optional params with empty description
     *
     * @param name Name of the option WITHOUT dash prefix
     */
    public void addOption(String name) {
        addOption(name, "", "", null, REQUIRED_NONE);
    }

    /**
     * Add supported option which is not required and has no bound action and no
     * optional params
     *
     * @param name Name of the option WITHOUT dash prefix
     * @param description Human readable description (will be used for usage
     * screen)
     */
    public void addOption(String name, String description) {
        addOption(name, description, "", null, REQUIRED_NONE);
    }

    /**
     * Add supported option which is not required and has no bound action
     *
     * @param name Name of the option WITHOUT dash prefix
     * @param description Human readable description (will be used for usage
     * screen)
     * @param argTypes Types of arguments s = string, i = integer, &lt;name&gt;
     * = human readable name, + repeat once and more, * repeat zero or more
     */
    public void addOption(String name, String description, String argTypes) {
        addOption(name, description, argTypes, null, REQUIRED_NONE);
    }

    /**
     * Add supported option which is not required
     *
     * @param name Name of the option WITHOUT dash prefix
     * @param description Human readable description (will be used for usage
     * screen)
     * @param argTypes Types of arguments s = string, i = integer, &lt;name&gt;
     * = human readable name, + repeat once and more, * repeat zero or more
     * @param action Action to do when the option is present
     */
    public void addOption(String name, String description, String argTypes, OptionActionListener action) {
        addOption(name, description, argTypes, action, REQUIRED_NONE);
    }

    /**
     * Add supported option
     *
     * @param name Name of the option WITHOUT dash prefix
     * @param description Human readable description (will be used for usage
     * screen)
     * @param argTypes Types of arguments s = string, i = integer, &lt;name&gt;
     * = human readable name, + repeat once and more, * repeat zero or more
     * @param action Action to do when the option is present
     * @param required Required? See REQUIRES_* constants
     */
    public void addOption(String name, String description, String argTypes, OptionActionListener action, int required) {
        if (argTypes == null) {
            argTypes = "";
        }
        checkArgType(argTypes);
        options.put(name, argTypes);
        actions.put(name, action);
        optionDescriptions.put(name, description);
        optionsOrder.add(name);
        if (required == REQUIRED_YES) {
            requiredOptions.add(name);
        }
        if (required == REQUIRED_ALONE) {
            standaloneOptions.add(name);
        }
    }

    public void setArgTypes(String type) {
        if (type == null) {
            type = "";
        }
        checkArgType(type);
        finalArgType = type;
    }

    protected int parseArg(String option, String args[], int i) {
        i++;
        String arg_types;
        String optionFullName;
        if (option == null) {
            arg_types = finalArgType;
            optionFullName = "Application ";
        } else {
            arg_types = options.get(option);
            optionFullName = "Option " + formatOptionName(option);
        }

        arg_types += "-"; //Final dot        

        List<Object> values = new ArrayList<>();
        //int nextarg = i + arg_types;
        //for (; i < nextarg; i++) {
        char lastType = '-';
        char lastModifier = '-';
        for (int j = 0; j < arg_types.length(); j++) {
            char c = arg_types.charAt(j);
            switch (c) {
                case ' ':
                    break;
                case '<':
                    j = arg_types.indexOf('>', j + 1);
                    break;
                case 's':
                case 'i':
                case '-':
                    if (lastType != '-' && i >= args.length) {
                        throw new IllegalArgumentException(optionFullName + " requires arguments:" + formatArgTypes(arg_types));
                    }
                    if ((lastModifier == '-' || lastModifier == '+') && lastType != '-') {
                        if (args[i].startsWith("-")) {
                            throw new IllegalArgumentException(optionFullName + " requires arguments:" + formatArgTypes(arg_types));
                        }
                        if (lastType == 's') {
                            values.add(args[i]);
                        }
                        if (lastType == 'i') {
                            try {
                                values.add(Integer.parseInt(args[i]));

                            } catch (NumberFormatException nfe) {
                                throw new IllegalArgumentException(optionFullName + " requires arguments:" + formatArgTypes(arg_types));
                            }
                        }
                        i++;
                        if (lastModifier == '+') {
                            lastModifier = '*';
                        }
                    }
                    if (lastModifier == '*') {
                        for (; i < args.length; i++) {
                            if (args[i].startsWith("-")) {
                                lastModifier = '-';
                                lastType = '-';
                                i--;
                            }
                            if (lastType == 'i') {
                                try {
                                    values.add(Integer.parseInt(args[i]));
                                } catch (NumberFormatException nfe) {
                                    lastModifier = '-';
                                    lastType = '-';
                                    i--;
                                }
                            } else if (lastType == 's') {
                                values.add(args[i]);
                            }
                        }
                    }
                    lastType = c;
                    break;

                case '+':
                case '*':
                    lastModifier = c;
                    break;
            }
        }
        if (option
                == null) {
            finalValues.addAll(values);
        } else {
            optionValues.put(option, values);
        }

        return i;
    }

    /**
     * Main method for argument parsing. The default error handler is printing
     * error and then usage to stderr.
     *
     * @param args Arguments of method main
     */
    public void parseArgs(String args[]) {
        parseArgs(args, (String message) -> {
            System.err.println(message);
            printUsage(System.err);
        });
    }

    /**
     * Formats types of arguments
     *
     * @param argTypes Types of arguments
     * @return Human readable string
     */
    protected String formatArgTypes(String argTypes) {
        String ret = "";
        argTypes += "-";
        String prevType = null;
        for (int i = 0; i < argTypes.length(); i++) {

            char c = argTypes.charAt(i);
            switch (c) {
                case ' ':
                    break;
                case '-':
                    if (prevType != null) {
                        ret += " " + prevType;
                    }
                    prevType = null;
                    break;
                case 's':
                    if (prevType != null) {
                        ret += " " + prevType;
                    }
                    prevType = "<string>";
                    break;
                case 'i':
                    if (prevType != null) {
                        ret += " " + prevType;
                    }
                    prevType = "<integer>";
                    break;
                case '<':
                    int i2 = argTypes.indexOf('>', i + 1);
                    if (i2 == -1) {
                        throw new IllegalArgumentException("Invalid arg type, unclosed parent");
                    }
                    prevType = "<" + argTypes.substring(i + 1, i2) + ">";
                    i = i2;
                    break;
                case '+':
                    if (prevType != null) {
                        ret += " " + prevType + " [" + prevType + "] ...";
                        prevType = null;
                    }
                    break;
                case '*':
                    if (prevType != null) {
                        ret += " [" + prevType + "] ...";
                        prevType = null;
                    }
                    break;
            }
        }
        return ret;
    }

    /**
     * Prints arguments usage to stdout
     */
    public void printUsage() {
        printUsage(System.out);
    }

    /**
     * Prints usage of the arguments
     *
     * @param out Any printstream, like System.out, System.err, etc...
     */
    public void printUsage(PrintStream out) {
        out.print("Usage: " + appCommandline);

        List<String> allOptions = new ArrayList<>(optionsOrder);

        List<String> required = new ArrayList<>();

        List<String> requiredGrouped = new ArrayList<>();

        //is it in the group?
        for (String[] group : groups) {
            String reqGroup = "";
            boolean allsingle = true;
            for (String goption : group) {
                if (goption.length() > 1) {
                    allsingle = false;
                    break;
                }
                if (!options.get(goption).isEmpty()) {
                    allsingle = false;
                    break;
                }
            }
            for (String goption : group) {
                if (allOptions.contains(goption)) {
                    if (!reqGroup.isEmpty() && !allsingle) {
                        reqGroup += "|";
                    }
                    if (allsingle) {
                        reqGroup += goption;
                    } else {
                        reqGroup += formatOptionName(goption) + formatArgTypes(options.get(goption));
                    }
                    requiredGrouped.add(goption);
                }
            }
            if (!reqGroup.isEmpty()) {
                if (allsingle) {
                    reqGroup = "-[" + reqGroup + "]";
                }
                required.add(reqGroup);
            }
        }
        for (String option : requiredOptions) {
            if (!requiredGrouped.contains(option)) {
                required.add(formatOptionName(option) + formatArgTypes(options.get(option)));
            }
        }
        for (String req : required) {
            out.print(" " + req);
        }
        out.println(formatArgTypes(finalArgType));
        for (String option : optionsOrder) {
            String name = formatOptionName(option);
            out.println("\t" + name + formatArgTypes(options.get(option)) + "\t" + optionDescriptions.get(option).replaceAll("\r?\n", "\n\t\t\t"));
        }
    }

    /**
     * Main method for argument parsing. Optional InvalidArgumentListener can be
     * passed to handle errors
     *
     * @param args Arguments of method main
     * @param listener Error listener
     */
    public void parseArgs(String args[], InvalidArgumentsListener listener) {
        try {
            optionValues.clear();
            finalValues.clear();

            boolean finishPart = false;
            for (int i = 0; i < args.length; i++) {
                String cur = args[i];
                if (!finishPart) {
                    if (cur.startsWith("--")) {
                        String name = cur.substring(2);
                        if (!options.containsKey(name)) {
                            throw new IllegalArgumentException("Unsupported option: " + cur);
                        }
                        i = parseArg(name, args, i) - 1;
                    } else if (cur.startsWith("-")) {
                        String combined = cur.substring(1);

                        for (char name : combined.toCharArray()) {
                            if (!options.containsKey("" + name)) {
                                throw new IllegalArgumentException("Unsupported option: -" + name);
                            }
                            String argTypes = options.get("" + name);
                            if (combined.length() > 1 && !argTypes.isEmpty()) {
                                throw new IllegalArgumentException("Option -" + name + " cannot be combined with others as it requires argument(s)");
                            }
                            i = parseArg("" + name, args, i) - 1;
                        }
                    } else if (!finalArgType.isEmpty()) {
                        finishPart = true;
                    } else {
                        throw new IllegalArgumentException("No additional arguments required");
                    }
                }
                if (finishPart) {
                    if (cur.startsWith("-")) {
                        throw new IllegalArgumentException("No option allowed in final part");
                    }
                    i = parseArg(null, args, i - 1);
                    if (i < args.length) {
                        throw new IllegalArgumentException("No additional arguments required");
                    }
                    break;
                }

            }

            for (String[] group : groups) {
                Set<String> used = new HashSet<>();
                for (String opt : group) {
                    if (optionValues.containsKey(opt)) {
                        used.add(opt);
                    }
                }
                if (used.size() > 1) {
                    String pr = "";
                    for (String opt : used) {
                        if (!pr.isEmpty()) {
                            pr += ", ";
                        }
                        pr += formatOptionName(opt);
                    }
                    throw new IllegalArgumentException("Options " + pr + " cannot be used together");
                }
            }

            boolean hasStandalone = false;
            for (String option : optionValues.keySet()) {
                if (standaloneOptions.contains(option)) {
                    hasStandalone = true;
                    break;
                }
            }

            if (!hasStandalone) {
                String missingRequired = "";
                loopreq:
                for (String re : requiredOptions) {
                    if (!optionValues.containsKey(re)) {
                        List<Integer> inGroups = new ArrayList<>();
                        loopgrp:
                        for (int g = 0; g < groups.size(); g++) {
                            String[] group = groups.get(g);
                            for (String goption : group) {
                                if (goption.equals(re)) {
                                    inGroups.add(g);
                                    break loopgrp;
                                }
                            }
                        }
                        if (!inGroups.isEmpty()) {
                            loopmatch:
                            for (int g : inGroups) {
                                String[] group = groups.get(g);
                                for (String goption : group) {
                                    if (optionValues.containsKey(goption)) {
                                        continue loopreq;
                                    }
                                }
                            }
                        }
                        if (!missingRequired.isEmpty()) {
                            missingRequired += ", ";
                        }
                        missingRequired += formatOptionName(re);
                    }
                }
                if (!missingRequired.isEmpty()) {
                    throw new IllegalArgumentException("Missing required options: " + missingRequired);
                }
            }

            //handle actions
            for (String s : optionValues.keySet()) {
                OptionActionListener li = actions.get(s);
                if (li != null) {
                    li.handleOption(s, getOptionValues(s), getOptionStrValues(s));
                }
            }
        } catch (IllegalArgumentException iex) {
            if (listener != null) {
                listener.handleInvalidArguments(iex.getMessage());
            } else {
                throw iex;
            }
        }
    }

}
