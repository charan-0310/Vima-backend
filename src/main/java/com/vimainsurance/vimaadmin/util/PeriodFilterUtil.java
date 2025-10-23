package com.vimainsurance.vimaadmin.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;

import lombok.Getter;
import lombok.Setter;

public class PeriodFilterUtil {
    
    public enum Period {
        THIS_MONTH("this_month"),
        LAST_MONTH("last_month"),
        LAST_3_MONTHS("last_3_months"),
        LAST_6_MONTHS("last_6_months"),
        THIS_YEAR("this_year");
        
        private final String value;
        
        Period(String value) {
            this.value = value;
        }
        
        public String getValue() {
            return value;
        }
        
        public static Period fromString(String period) {
            for (Period p : Period.values()) {
                if (p.value.equals(period)) {
                    return p;
                }
            }
            throw new IllegalArgumentException("Invalid period: " + period);
        }
    }
    
    @Getter
    @Setter
    public static class PeriodRange {
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private LocalDateTime previousStartDate;
        private LocalDateTime previousEndDate;
        
        public PeriodRange(LocalDateTime startDate, LocalDateTime endDate, 
                          LocalDateTime previousStartDate, LocalDateTime previousEndDate) {
            this.startDate = startDate;
            this.endDate = endDate;
            this.previousStartDate = previousStartDate;
            this.previousEndDate = previousEndDate;
        }
    }
    
    public static PeriodRange getPeriodRange(String period) {
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        
        switch (Period.fromString(period)) {
            case THIS_MONTH:
                return getThisMonthRange(today);
            case LAST_MONTH:
                return getLastMonthRange(today);
            case LAST_3_MONTHS:
                return getLast3MonthsRange(today);
            case LAST_6_MONTHS:
                return getLast6MonthsRange(today);
            case THIS_YEAR:
                return getThisYearRange(today);
            default:
                throw new IllegalArgumentException("Unsupported period: " + period);
        }
    }
    
    private static PeriodRange getThisMonthRange(LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today);
        LocalDateTime startDate = currentMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = currentMonth.atEndOfMonth().atTime(23, 59, 59);
        
        YearMonth previousMonth = currentMonth.minusMonths(1);
        LocalDateTime previousStartDate = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime previousEndDate = previousMonth.atEndOfMonth().atTime(23, 59, 59);
        
        return new PeriodRange(startDate, endDate, previousStartDate, previousEndDate);
    }
    
    private static PeriodRange getLastMonthRange(LocalDate today) {
        YearMonth lastMonth = YearMonth.from(today).minusMonths(1);
        LocalDateTime startDate = lastMonth.atDay(1).atStartOfDay();
        LocalDateTime endDate = lastMonth.atEndOfMonth().atTime(23, 59, 59);
        
        YearMonth previousMonth = lastMonth.minusMonths(1);
        LocalDateTime previousStartDate = previousMonth.atDay(1).atStartOfDay();
        LocalDateTime previousEndDate = previousMonth.atEndOfMonth().atTime(23, 59, 59);
        
        return new PeriodRange(startDate, endDate, previousStartDate, previousEndDate);
    }
    
    private static PeriodRange getLast3MonthsRange(LocalDate today) {
        LocalDateTime endDate = today.atTime(23, 59, 59);
        LocalDateTime startDate = today.minusMonths(3).atStartOfDay();
        
        LocalDateTime previousEndDate = startDate.minusDays(1).toLocalDate().atTime(23, 59, 59);
        LocalDateTime previousStartDate = today.minusMonths(6).atStartOfDay();
        
        return new PeriodRange(startDate, endDate, previousStartDate, previousEndDate);
    }
    
    private static PeriodRange getLast6MonthsRange(LocalDate today) {
        LocalDateTime endDate = today.atTime(23, 59, 59);
        LocalDateTime startDate = today.minusMonths(6).atStartOfDay();
        
        LocalDateTime previousEndDate = startDate.minusDays(1).toLocalDate().atTime(23, 59, 59);
        LocalDateTime previousStartDate = today.minusMonths(12).atStartOfDay();
        
        return new PeriodRange(startDate, endDate, previousStartDate, previousEndDate);
    }
    
    private static PeriodRange getThisYearRange(LocalDate today) {
        LocalDateTime startDate = today.withDayOfYear(1).atStartOfDay();
        LocalDateTime endDate = today.atTime(23, 59, 59);
        
        LocalDate previousYear = today.minusYears(1);
        LocalDateTime previousStartDate = previousYear.withDayOfYear(1).atStartOfDay();
        LocalDateTime previousEndDate = previousYear.withDayOfYear(previousYear.lengthOfYear()).atTime(23, 59, 59);
        
        return new PeriodRange(startDate, endDate, previousStartDate, previousEndDate);
    }
    
    public static double calculateGrowth(double currentValue, double previousValue) {
        if (previousValue == 0) {
            return currentValue > 0 ? 100.0 : 0.0;
        }
        return ((currentValue - previousValue) / previousValue) * 100;
    }
    
    public static double calculateConversionRate(int quotes, int leads) {
        return leads > 0 ? (double) quotes / leads * 100 : 0.0;
    }
    
    public static double calculateClosingRate(int policies, int quotes) {
        return quotes > 0 ? (double) policies / quotes * 100 : 0.0;
    }
}
