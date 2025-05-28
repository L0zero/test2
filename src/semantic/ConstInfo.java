package semantic;

public class ConstInfo extends Info {

    private String value = null;

    public ConstInfo(String ident) {
        super(ident);
        addDecoration("Const");
    }

    public String getValue() {
        return value;
    }

    public int getValue(int p) {
        if (value.startsWith("\"")) {
            String t = value.substring(1, value.length() - 1);
            int i = 0, j = 0;
            while (j < p) {
                if (t.length() <= i) {
                    return 0;
                }
                if (t.charAt(i) == '\\') {
                    i++;
                }
                i++;
                j++;
            }
            if (t.length() <= i) {
                return 0;
            }
            if (t.charAt(i) == '\\') {
                return getCharValue(t.substring(i, i+2));
            } else {
                return getCharValue(t.substring(i, i+1));
            }
        } else if (value.startsWith("{")) {
            String s = value.substring(1, value.length()-1);
            String[] ss = s.split(",");
            if (ss.length <= p) {
                return 0;
            }
            if (ss[p].trim().isEmpty()) {
                return 0;
            }
            return Integer.parseInt(ss[p].trim());
        }
        if (value.isEmpty()) {
            return 0;
        }
        return Integer.parseInt(value);
    }

    public void setValue(String value) {
        this.value = value;
    }

    private static int getCharValue(String s) {
        if (s.length() == 1) {
            return s.charAt(0);
        } else {
            switch (s.charAt(1)) {
                case 'a':
                    return 7;
                case 'b':
                    return 8;
                case 't':
                    return 9;
                case 'n':
                    return 10;
                case 'v':
                    return 11;
                case 'f':
                    return 12;
                case '\"':
                    return 34;
                case '\'':
                    return 39;
                case '\\':
                    return 92;
                case '0':
                    return 0;
                default:
                    return 0;
            }
        }
    }
}
