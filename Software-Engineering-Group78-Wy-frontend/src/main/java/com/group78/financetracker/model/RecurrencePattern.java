package com.group78.financetracker.model;

import java.time.LocalDate;

public enum RecurrencePattern {
    DAILY("Daily") {
        @Override
        public LocalDate getNextDate(LocalDate currentDate) {
            return currentDate.plusDays(1);
        }
    },
    WEEKLY("Weekly") {
        @Override
        public LocalDate getNextDate(LocalDate currentDate) {
            return currentDate.plusWeeks(1);
        }
    },
    MONTHLY("Monthly") {
        @Override
        public LocalDate getNextDate(LocalDate currentDate) {
            return currentDate.plusMonths(1);
        }
    },
    QUARTERLY("Quarterly") {
        @Override
        public LocalDate getNextDate(LocalDate currentDate) {
            return currentDate.plusMonths(3);
        }
    },
    YEARLY("Yearly") {
        @Override
        public LocalDate getNextDate(LocalDate currentDate) {
            return currentDate.plusYears(1);
        }
    };

    private final String displayName;

    RecurrencePattern(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }

    public abstract LocalDate getNextDate(LocalDate currentDate);
} 