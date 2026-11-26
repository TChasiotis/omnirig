package com.omnirig.services.analyzer;

import com.omnirig.models.Category;
import com.omnirig.models.Product;
import com.omnirig.services.BuildAnalyzer.FpsSet;
import com.omnirig.services.CompatibilityEngine;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.omnirig.services.CompatibilityEngine.*;

public final class TechnicianNotesGenerator {

    private TechnicianNotesGenerator() {}

    public static List<String> generate(
            Map<Category, Product> build,
            double cpuScore,
            double difference,
            int vram,
            FpsSet fps4k) {

        Product cpu = build.get(Category.CPU);
        Product gpu = build.get(Category.GPU);
        Product ramProduct = build.get(Category.RAM);
        Product psu = build.get(Category.PSU);
        Product storage = build.get(Category.STORAGE);
        Product cooler = build.get(Category.COOLER);
        Product pcCase = build.get(Category.CASE);

        List<String> notes = new ArrayList<>(CompatibilityEngine.getCompatibilityNotes(build));

        if (gpu != null) {
            if (fps4k.cyberpunk() < 25) {
                notes.add(
                    "Το Cyberpunk 2077 σε native 4K αναμένεται να έχει χαμηλή απόδοση. " +
                    "Συνιστώνται χαμηλότερη ανάλυση ή upscaling."
                );
            } else if (fps4k.cyberpunk() < 40) {
                notes.add(
                    "Το Cyberpunk 2077 σε native 4K θα χρειαστεί συμβιβασμούς στις ρυθμίσεις για ομαλότερη εμπειρία."
                );
            } else if (fps4k.cyberpunk() < 60) {
                notes.add(
                    "Το σύστημα μπορεί να τρέξει Cyberpunk σε 4K, αλλά δεν αναμένεται σταθερή εμπειρία 60 FPS."
                );
            } else {
                notes.add(
                    "Η εκτιμώμενη απόδοση στο Cyberpunk 4K είναι ικανοποιητική σε σχέση με τον στόχο των 60 FPS."
                );
            }

            if (vram < 8) {
                notes.add(
                    "Η διαθέσιμη VRAM μπορεί να περιορίσει τις επιλογές texture quality σε υψηλές αναλύσεις."
                );
            }
        }

        if (cpu != null && cpuScore < 4.5) {
            notes.add(
                "Ο επεξεργαστής είναι σχετικά αδύναμος για σύγχρονο gaming και μπορεί να περιορίσει τα FPS."
            );
        }

        if (cpu != null && gpu != null && difference < -1.5) {
            notes.add(
                "Ένας ισχυρότερος επεξεργαστής θα μπορούσε να αξιοποιήσει καλύτερα την κάρτα γραφικών."
            );
        }

        if (ramProduct != null) {
            int capacity = getInt(ramProduct, "capacity_gb", 16);
            int modules = getInt(ramProduct, "modules", 1);

            if (capacity < 16) {
                notes.add(
                    "Τα λιγότερα από 16GB RAM ενδέχεται να προκαλέσουν stuttering σε σύγχρονα παιχνίδια."
                );
            }
            if (modules == 1) {
                notes.add(
                    "Η RAM λειτουργεί πιθανότατα σε single-channel διάταξη. " +
                    "Ένα δεύτερο συμβατό module μπορεί να βελτιώσει τις επιδόσεις."
                );
            }
            if (capacity >= 32) {
                notes.add(
                    "Τα " + capacity + "GB RAM προσφέρουν άνεση για σύγχρονο gaming και multitasking."
                );
            }
        }

        if (storage != null) {
            String interfaceType = getString(storage, "interface").toUpperCase();
            if (interfaceType.contains("SATA")) {
                notes.add(
                    "Ο SATA SSD είναι επαρκής για gaming. " +
                    "Ένας NVMe SSD θα βελτίωνε κυρίως τους χρόνους φόρτωσης, όχι ουσιαστικά τα FPS."
                );
            }
        }

        if (psu != null && gpu != null) {
            int wattage = getInt(psu, "wattage", 550);
            int recommended = getInt(gpu, "recommended_psu_w", 0);
            int estimated = estimateSystemWattage(build);

            if (recommended > 0 && wattage < recommended) {
                notes.add(
                    "Προσοχή: Το PSU είναι χαμηλότερο από τη συνιστώμενη ισχύ τροφοδοτικού της GPU."
                );
            } else if (wattage < estimated * 1.15) {
                notes.add(
                    "Το PSU έχει περιορισμένο περιθώριο ισχύος για το συγκεκριμένο σύστημα."
                );
            } else {
                notes.add(
                    "Το τροφοδοτικό καλύπτει τις εκτιμώμενες ανάγκες του συστήματος."
                );
            }
        }

        if (cooler != null && cpu != null) {
            int cpuTdp = getInt(cpu, "tdp_max_w", 65);
            int coolerTdp = getInt(cooler, "max_tdp_w", 150);

            if (coolerTdp < cpuTdp) {
                notes.add(
                    "Η ψύκτρα έχει χαμηλότερη ονομαστική ικανότητα ψύξης από το TDP του επεξεργαστή."
                );
            } else {
                notes.add(
                    "Η ψύκτρα διαθέτει επαρκή ονομαστική ικανότητα για το TDP του CPU."
                );
            }
        }

        if (pcCase != null) {
            int includedFans = getInt(pcCase, "included_fans", 0);
            if (includedFans == 0) {
                notes.add(
                    "Το κουτί δεν περιλαμβάνει εργοστασιακούς ανεμιστήρες. " +
                    "Συνιστάται η προσθήκη ανεμιστήρων για σωστό airflow."
                );
            } else if (includedFans >= 3) {
                notes.add(
                    "Το κουτί διαθέτει πολλούς εργοστασιακούς ανεμιστήρες, κάτι που μπορεί να βοηθήσει στη θερμική διαχείριση."
                );
            }
        }

        notes.add(
            "Οι εκτιμήσεις FPS αφορούν High preset, native resolution, " +
            "Ray Tracing OFF και χωρίς DLSS/FSR. " +
            "Αποτελούν προσεγγιστικές εκτιμήσεις και όχι εγγυημένες μετρήσεις."
        );

        return notes;
    }
}