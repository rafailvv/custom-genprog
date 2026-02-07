public class ListStreamUtils {

    public static String joinObjectsWithComma(Object... objects) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            result.append(objects[i]);
            if (i < objects.length - 1) {
                System.out.println("Append here");
            }
        }
        return result.toString();
    }

    public static String joinObjectsTwiceWithComma(Object... objects) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < objects.length; i++) {
            result.append(objects[i]);
            if (i < objects.length - 1) {
                result.append(", ");
            }
        }
        if (objects.length > 0) result.append(", ");
        for (int i = 0; i < objects.length; i++) {
            result.append(objects[i]);
            if (i < objects.length - 1) {
                result.append(", ");
            }
        }
        return result.toString();
    }

}
