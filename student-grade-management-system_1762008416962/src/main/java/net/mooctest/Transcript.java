package net.mooctest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Transcript {
    public static class LineItem {
        private final String courseCode;
        private final String courseTitle;
        private final int creditHours;
        private final double percentage;
        private final double gpaPoints;

        public LineItem(String courseCode, String courseTitle, int creditHours, double percentage, double gpaPoints) {
            this.courseCode = courseCode;
            this.courseTitle = courseTitle;
            this.creditHours = creditHours;
            this.percentage = percentage;
            this.gpaPoints = gpaPoints;
        }

        public String getCourseCode() { return courseCode; }
        public String getCourseTitle() { return courseTitle; }
        public int getCreditHours() { return creditHours; }
        public double getPercentage() { return percentage; }
        public double getGpaPoints() { return gpaPoints; }
    }

    private final List<LineItem> items;

    public Transcript() {
        this.items = new ArrayList<>();
    }

    public void addItem(LineItem item) {
        if (item == null) throw new ValidationException("item must not be null");
        items.add(item);
    }

    public List<LineItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public double computeCumulativeGpa() {
        double totalQualityPoints = 0.0;
        int totalCredits = 0;
        for (LineItem item : items) {
            totalQualityPoints += item.getGpaPoints() * item.getCreditHours();
            totalCredits += item.getCreditHours();
        }
        if (totalCredits == 0) return 0.0;
        return totalQualityPoints / totalCredits;
    }
}


