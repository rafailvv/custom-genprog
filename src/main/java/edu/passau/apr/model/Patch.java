package edu.passau.apr.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a patch as a collection of edits.
 * A patch is applied to the buggy source code to create a candidate fix.
 */
public class Patch {
    private final List<Edit> edits;

    public Patch() {
        this.edits = new ArrayList<>();
    }

    public Patch(List<Edit> edits) {
        this.edits = new ArrayList<>(edits);
    }

    public void addEdit(Edit edit) {
        edits.add(edit);
    }

    public void removeEdit(int index) {
        if (index >= 0 && index < edits.size()) {
            edits.remove(index);
        }
    }

    public List<Edit> getEdits() {
        return new ArrayList<>(edits);
    }

    /**
     * Sort edits by line number in descending order to avoid index shifting issues
     *
     * @return all edits in descending order
     */
    public List<Edit> getSortedEdits() {
        return getEdits()
                .stream()
                .sorted((a, b) -> Integer.compare(b.lineNumber(), a.lineNumber()))
                .toList();
    }

    public int getEditCount() {
        return edits.size();
    }

    public boolean isEmpty() {
        return edits.isEmpty();
    }

    /**
     * Creates a copy of this patch.
     */
    public Patch copy() {
        return new Patch(this.edits);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Edit edit : edits) {
            sb.append(edit.toString()).append("\n");
        }
        return sb.toString();
    }

    public List<String> applyTo(List<String> originalLines) {
        List<String> result = new ArrayList<>(originalLines);

        for (Edit edit : getSortedEdits()) {
            int lineIndex = edit.lineNumber() - 1;

            if (lineIndex < 0 || lineIndex >= result.size()) {
                continue;
            }

            switch (edit.type()) {
                case DELETE:
                    result.remove(lineIndex);
                    break;

                case INSERT:
                    if (edit.content() != null) {
                        result.add(lineIndex, edit.content());
                    }
                    break;

                case REPLACE:
                    result.remove(lineIndex);
                    if (edit.content() != null) {
                        result.add(lineIndex, edit.content());
                    }
                    break;
            }
        }

        return result;
    }
}

