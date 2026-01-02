public class StringUtils {
    public static boolean isEmpty(String str) {
        return str == null || str.length() == 0;
    }

    public static boolean equals(String a, String b) {
        if (a == null && b == null) {
            return true;
        }
        if (a == null || b == null) {
            return false;
        }
        return a == b;
    }
}

