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
}

