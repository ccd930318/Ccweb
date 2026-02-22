package com.ccweb.babytracker.growth;

import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculates WHO growth percentiles using the LMS (Box-Cox) method.
 * Reference data is loaded from CSV files in resources/who/.
 * Returns null when no reference data exists for the given age/sex.
 */
@Service
public class WhoPercentileService {

    private record LmsKey(String sex, String measure, int ageMonths) {}
    private record Lms(double l, double m, double s) {}

    private final Map<LmsKey, Lms> table = new HashMap<>();

    @PostConstruct
    void load() {
        for (String measure : new String[]{"weight", "height", "head"}) {
            for (String sex : new String[]{"M", "F"}) {
                String path = "who/" + measure + "_" + sex.toLowerCase() + ".csv";
                try (var reader = new BufferedReader(
                        new InputStreamReader(new ClassPathResource(path).getInputStream()))) {
                    reader.lines().skip(1).forEach(line -> {
                        String[] parts = line.split(",");
                        if (parts.length < 4) return;
                        int month = Integer.parseInt(parts[0].trim());
                        double l = Double.parseDouble(parts[1].trim());
                        double m = Double.parseDouble(parts[2].trim());
                        double s = Double.parseDouble(parts[3].trim());
                        table.put(new LmsKey(sex, measure, month), new Lms(l, m, s));
                    });
                } catch (Exception ignored) {
                    // CSV absent — percentiles will be null
                }
            }
        }
    }

    public Double percentile(String sex, String measure, int ageMonths, BigDecimal value) {
        if (value == null) return null;
        Lms lms = table.get(new LmsKey(sex, measure, ageMonths));
        if (lms == null) return null;
        double z = zscore(lms, value.doubleValue());
        return normalCdf(z) * 100.0;
    }

    private double zscore(Lms lms, double x) {
        if (Math.abs(lms.l()) < 1e-9) {
            return Math.log(x / lms.m()) / lms.s();
        }
        return (Math.pow(x / lms.m(), lms.l()) - 1.0) / (lms.l() * lms.s());
    }

    /** Abramowitz & Stegun approximation, error < 1.5e-7 */
    private double normalCdf(double z) {
        double t = 1.0 / (1.0 + 0.2316419 * Math.abs(z));
        double poly = t * (0.319381530
                + t * (-0.356563782
                + t * (1.781477937
                + t * (-1.821255978
                + t * 1.330274429))));
        double pdf = Math.exp(-0.5 * z * z) / Math.sqrt(2 * Math.PI);
        double p = 1.0 - pdf * poly;
        return z >= 0 ? p : 1.0 - p;
    }
}
