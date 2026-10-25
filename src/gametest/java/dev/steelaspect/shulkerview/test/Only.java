package dev.steelaspect.shulkerview.test;

/** -Ponly=<name> runs just that test (preview, settings, view, named, vanilla, mixed, ender). */
final class Only {
    private Only() {}

    static boolean skip(String name) {
        String only = System.getProperty("shulkerview.only");
        return only != null && !only.equals(name);
    }
}
