package com.worktree.secure.app;

import java.util.ArrayList;
import java.util.List;

public class ScreenshotSession {

    private final List<String> screenshots = new ArrayList<>();

    public void add(String base64) {
        screenshots.add(base64);
    }

    public void remove(int index) {
        if (index >= 0 && index < screenshots.size()) {
            screenshots.remove(index);
        }
    }

    public boolean isEmpty() {
        return screenshots.isEmpty();
    }

    public List<String> getAll() {
        return screenshots;
    }

    public void clear() {
        screenshots.clear();
    }
}
