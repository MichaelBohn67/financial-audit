package de.bohnottensen.financialaudit.infrastructure.initialization;

import de.bohnottensen.financialaudit.domain.model.Industriekontenrahmen;
import de.bohnottensen.financialaudit.infrastructure.persistence.IndustriekontenrahmenRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Initialisiert und persistiert den Industriekontenrahmen (IKR) in der Datenbank.
 */
@Component
@Order(20)
public class IndustriekontenrahmenInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(IndustriekontenrahmenInitializer.class);

    private final IndustriekontenrahmenRepository repository;

    public IndustriekontenrahmenInitializer(IndustriekontenrahmenRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initializeIndustriekontenrahmen();
    }

    /**
     * Fügt den vollständigen Industriekontenrahmen zur Datenbank hinzu, sofern noch nicht vorhanden.
     *
     * @return Liste der neu erstellten oder bereits vorhandenen IKR-Konten
     */
    @Transactional
    public List<Industriekontenrahmen> initializeIndustriekontenrahmen() {
        log.info("Prüfe und initialisiere den Industriekontenrahmen (IKR)...");
        List<Industriekontenrahmen> defaultAccounts = createDefaultIkrAccounts();
        List<Industriekontenrahmen> persistedAccounts = new ArrayList<>();

        for (Industriekontenrahmen account : defaultAccounts) {
            if (!repository.existsByAccountNumber(account.getAccountNumber())) {
                persistedAccounts.add(repository.save(account));
            } else {
                repository.findByAccountNumber(account.getAccountNumber()).ifPresent(persistedAccounts::add);
            }
        }

        log.info("Industriekontenrahmen initialisiert: {} Konten vorhanden.", persistedAccounts.size());
        return persistedAccounts;
    }

    /**
     * Alias-Methode zum Hinzufügen des Industriekontenrahmens zur Datenbank.
     */
    @Transactional
    public List<Industriekontenrahmen> addIndustriekontenrahmen() {
        return initializeIndustriekontenrahmen();
    }

    /**
     * Erstellt die Standard-Kontenliste des Industriekontenrahmens (IKR, Kontenklassen 0 bis 9).
     */
    public static List<Industriekontenrahmen> createDefaultIkrAccounts() {
        List<Industriekontenrahmen> list = new ArrayList<>();

        // Kontenklasse 0: Immaterielle Vermögensgegenstände und Sachanlagen
        list.add(new Industriekontenrahmen("0110", "Konzessionen, gewerbliche Schutzrechte und Lizenzen", 0, "01 Immaterielle Vermögensgegenstände", "AKTIV", "Konzessionen und gewerbliche Schutzrechte"));
        list.add(new Industriekontenrahmen("0120", "Geschäfts- oder Firmenwert", 0, "01 Immaterielle Vermögensgegenstände", "AKTIV", "Derivativer Goodwill"));
        list.add(new Industriekontenrahmen("0130", "Geleistete Anzahlungen auf immaterielle Vermögensgegenstände", 0, "01 Immaterielle Vermögensgegenstände", "AKTIV", "Anzahlungen"));
        list.add(new Industriekontenrahmen("0200", "Unbebaute Grundstücke", 0, "02 Grundstücke und grundstücksgleiche Rechte", "AKTIV", "Unbebaute Grundstücke"));
        list.add(new Industriekontenrahmen("0210", "Bebaute Grundstücke (Grundstücksanteil)", 0, "02 Grundstücke und grundstücksgleiche Rechte", "AKTIV", "Grund und Boden bebauter Grundstücke"));
        list.add(new Industriekontenrahmen("0240", "Betriebsgebäude", 0, "02 Grundstücke und Bauten", "AKTIV", "Fabrik- und Werkstattgebäude"));
        list.add(new Industriekontenrahmen("0250", "Verwaltungsgebäude", 0, "02 Grundstücke und Bauten", "AKTIV", "Büro- und Verwaltungsgebäude"));
        list.add(new Industriekontenrahmen("0500", "Technische Anlagen und Maschinen", 0, "05 Technische Anlagen und Maschinen", "AKTIV", "Produktionsmaschinen und Anlagen"));
        list.add(new Industriekontenrahmen("0700", "Andere Anlagen, Betriebs- und Geschäftsausstattung", 0, "07 Andere Anlagen", "AKTIV", "Sonstige Betriebsanlagen"));
        list.add(new Industriekontenrahmen("0840", "Fuhrpark", 0, "08 Betriebs- und Geschäftsausstattung", "AKTIV", "Lkw, Pkw und Transportfahrzeuge"));
        list.add(new Industriekontenrahmen("0860", "Büromöbel und EDV-Ausstattung", 0, "08 Betriebs- und Geschäftsausstattung", "AKTIV", "Büroausstattung, Server, Computer"));
        list.add(new Industriekontenrahmen("0870", "Werkzeuge und Kleingeräte", 0, "08 Betriebs- und Geschäftsausstattung", "AKTIV", "Betriebswerkzeuge"));
        list.add(new Industriekontenrahmen("0900", "Geleistete Anzahlungen und Anlagen im Bau", 0, "09 Anzahlungen und Anlagen im Bau", "AKTIV", "Anzahlungen auf Sachanlagen"));

        // Kontenklasse 1: Finanzanlagen
        list.add(new Industriekontenrahmen("1310", "Beteiligungen an verbundenen Unternehmen", 1, "13 Beteiligungen", "AKTIV", "Beteiligungen an Tochtergesellschaften"));
        list.add(new Industriekontenrahmen("1340", "Beteiligungen an assoziierten Unternehmen", 1, "13 Beteiligungen", "AKTIV", "Assoziierte Beteiligungen"));
        list.add(new Industriekontenrahmen("1400", "Ausleihungen an verbundene Unternehmen", 1, "14 Ausleihungen", "AKTIV", "Langfristige Darlehen an verbundene Unternehmen"));
        list.add(new Industriekontenrahmen("1500", "Wertpapiere des Anlagevermögens", 1, "15 Wertpapiere des Anlagevermögens", "AKTIV", "Festverzinsliche Wertpapiere des AV"));
        list.add(new Industriekontenrahmen("1600", "Sonstige Ausleihungen", 1, "16 Sonstige Finanzanlagen", "AKTIV", "Sonstige langfristige Ausleihungen"));

        // Kontenklasse 2: Umlaufvermögen und aktive Rechnungsabgrenzung
        list.add(new Industriekontenrahmen("2000", "Rohstoffe / Fertigungsmaterial", 2, "20 Roh-, Hilfs- und Betriebsstoffe", "AKTIV", "Hauptmaterial für Produktion"));
        list.add(new Industriekontenrahmen("2010", "Vorprodukte / Fremdbauteile", 2, "20 Roh-, Hilfs- und Betriebsstoffe", "AKTIV", "Zugekaufte Komponenten"));
        list.add(new Industriekontenrahmen("2020", "Hilfsstoffe", 2, "20 Roh-, Hilfs- und Betriebsstoffe", "AKTIV", "Hilfsstoffe für Fertigung"));
        list.add(new Industriekontenrahmen("2030", "Betriebsstoffe", 2, "20 Roh-, Hilfs- und Betriebsstoffe", "AKTIV", "Öle, Energie, Treibstoffe"));
        list.add(new Industriekontenrahmen("2100", "Unfertige Erzeugnisse, unfertige Leistungen", 2, "21 Unfertige Erzeugnisse", "AKTIV", "Bestand an unfertigen Erzeugnissen"));
        list.add(new Industriekontenrahmen("2200", "Fertige Erzeugnisse und Waren", 2, "22 Fertige Erzeugnisse und Waren", "AKTIV", "Verkaufsbereite Fertigerzeugnisse"));
        list.add(new Industriekontenrahmen("2300", "Geleistete Anzahlungen auf Vorräte", 2, "23 Geleistete Anzahlungen", "AKTIV", "Anzahlungen auf Vorratsvermögen"));
        list.add(new Industriekontenrahmen("2400", "Forderungen aus Lieferungen und Leistungen (LuL)", 2, "24 Forderungen", "AKTIV", "Kundenforderungen / Debitoren"));
        list.add(new Industriekontenrahmen("2450", "Wechsel-Forderungen", 2, "24 Forderungen", "AKTIV", "Besitzwechsel"));
        list.add(new Industriekontenrahmen("2470", "Zweifelhafte Forderungen", 2, "24 Forderungen", "AKTIV", "Wertberichtigte / zweifelhafte Kundenforderungen"));
        list.add(new Industriekontenrahmen("2600", "Vorsteuer 19%", 2, "26 Sonstige Vermögensgegenstände", "AKTIV", "Abziehbare Vorsteuer 19%"));
        list.add(new Industriekontenrahmen("2605", "Vorsteuer 7%", 2, "26 Sonstige Vermögensgegenstände", "AKTIV", "Abziehbare Vorsteuer 7%"));
        list.add(new Industriekontenrahmen("2640", "Sonstige Vermögensgegenstände", 2, "26 Sonstige Vermögensgegenstände", "AKTIV", "Übrige sonstige Forderungen"));
        list.add(new Industriekontenrahmen("2700", "Wertpapiere des Umlaufvermögens", 2, "27 Wertpapiere des Umlaufvermögens", "AKTIV", "Kurzfristige Wertpapiere"));
        list.add(new Industriekontenrahmen("2800", "Guthaben bei Kreditinstituten (Bank)", 2, "28 Flüssige Mittel", "AKTIV", "Bankkontokorrentguthaben"));
        list.add(new Industriekontenrahmen("2860", "Postbank", 2, "28 Flüssige Mittel", "AKTIV", "Postbankkonto"));
        list.add(new Industriekontenrahmen("2880", "Kasse", 2, "28 Flüssige Mittel", "AKTIV", "Bargeldbestand / Geschäftskasse"));
        list.add(new Industriekontenrahmen("2900", "Aktive Rechnungsabgrenzung (ARAP)", 2, "29 Rechnungsabgrenzungsposten", "AKTIV", "Aktive Rechnungsabgrenzungsposten"));

        // Kontenklasse 3: Eigenkapital, Wertberichtigungen und Rückstellungen
        list.add(new Industriekontenrahmen("3000", "Gezeichnetes Kapital / Stammkapital", 3, "30 Eigenkapital", "PASSIV", "Stammkapital / Grundkapital"));
        list.add(new Industriekontenrahmen("3100", "Kapitalrücklage", 3, "31 Kapitalrücklage", "PASSIV", "Kapitalrücklagen nach § 272 HGB"));
        list.add(new Industriekontenrahmen("3300", "Gesetzliche Gewinnrücklage", 3, "33 Gewinnrücklagen", "PASSIV", "Gesetzliche Rücklage"));
        list.add(new Industriekontenrahmen("3340", "Andere Gewinnrücklagen", 3, "33 Gewinnrücklagen", "PASSIV", "Freie Gewinnrücklagen"));
        list.add(new Industriekontenrahmen("3680", "Gewinnvortrag / Verlustvortrag", 3, "36 Ergebnisvortrag", "PASSIV", "Ergebnisvortrag aus Vorjahren"));
        list.add(new Industriekontenrahmen("3700", "Jahresüberschuss / Jahresfehlbetrag", 3, "37 Jahresergebnis", "PASSIV", "Laufendes Jahresergebnis"));
        list.add(new Industriekontenrahmen("3900", "Steuerrückstellungen", 3, "39 Rückstellungen", "PASSIV", "Rückstellungen für Ertragsteuern"));
        list.add(new Industriekontenrahmen("3920", "Pensionsrückstellungen", 3, "39 Rückstellungen", "PASSIV", "Rückstellungen für Altersversorgungsverpflichtungen"));
        list.add(new Industriekontenrahmen("3940", "Sonstige Rückstellungen", 3, "39 Rückstellungen", "PASSIV", "Rückstellungen für Ungewisse Verbindlichkeiten"));

        // Kontenklasse 4: Verbindlichkeiten und passive Rechnungsabgrenzung
        list.add(new Industriekontenrahmen("4200", "Verbindlichkeiten gegenüber Kreditinstituten", 4, "42 Finanzverbindlichkeiten", "PASSIV", "Bankdarlehen und Kontokorrentkredite"));
        list.add(new Industriekontenrahmen("4300", "Erhaltene Anzahlungen auf Bestellungen", 4, "43 Erhaltene Anzahlungen", "PASSIV", "Anzahlungen von Kunden"));
        list.add(new Industriekontenrahmen("4400", "Verbindlichkeiten aus Lieferungen und Leistungen (LuL)", 4, "44 Verbindlichkeiten LuL", "PASSIV", "Lieferantenverbindlichkeiten / Kreditoren"));
        list.add(new Industriekontenrahmen("4500", "Verbindlichkeiten aus Wechseln", 4, "45 Wechselverbindlichkeiten", "PASSIV", "Schuldwechsel"));
        list.add(new Industriekontenrahmen("4800", "Umsatzsteuer 19%", 4, "48 Sonstige Verbindlichkeiten", "PASSIV", "Geschuldete Umsatzsteuer 19%"));
        list.add(new Industriekontenrahmen("4805", "Umsatzsteuer 7%", 4, "48 Sonstige Verbindlichkeiten", "PASSIV", "Geschuldete Umsatzsteuer 7%"));
        list.add(new Industriekontenrahmen("4830", "Sonstige Verbindlichkeiten gegenüber Finanzamt", 4, "48 Sonstige Verbindlichkeiten", "PASSIV", "Lohn- und Kirchensteuer"));
        list.add(new Industriekontenrahmen("4840", "Verbindlichkeiten im Rahmen der sozialen Sicherheit", 4, "48 Sonstige Verbindlichkeiten", "PASSIV", "Krankenkassen- und Sozialversicherungsbeiträge"));
        list.add(new Industriekontenrahmen("4890", "Sonstige Verbindlichkeiten", 4, "48 Sonstige Verbindlichkeiten", "PASSIV", "Übrige Verbindlichkeiten"));
        list.add(new Industriekontenrahmen("4900", "Passive Rechnungsabgrenzung (PRAP)", 4, "49 Rechnungsabgrenzungsposten", "PASSIV", "Passive Rechnungsabgrenzungsposten"));

        // Kontenklasse 5: Erträge
        list.add(new Industriekontenrahmen("5000", "Umsatzerlöse für eigene Erzeugnisse", 5, "50 Umsatzerlöse", "ERTRAG", "Umsatzerlöse aus eigener Fertigung"));
        list.add(new Industriekontenrahmen("5100", "Umsatzerlöse für Waren", 5, "51 Umsatzerlöse Waren", "ERTRAG", "Handelswarenerlöse"));
        list.add(new Industriekontenrahmen("5200", "Bestandsveränderungen", 5, "52 Bestandsveränderungen", "ERTRAG", "Erhöhung oder Verminderung des Bestands"));
        list.add(new Industriekontenrahmen("5300", "Andere aktivierte Eigenleistungen", 5, "53 Aktivierte Eigenleistungen", "ERTRAG", "Selbsterstellte Anlagen"));
        list.add(new Industriekontenrahmen("5400", "Sonstige betriebliche Erträge", 5, "54 Sonstige betriebliche Erträge", "ERTRAG", "Nebenerlöse, Mieteinnahmen"));
        list.add(new Industriekontenrahmen("5410", "Erlöse aus Anlagenabgängen", 5, "54 Sonstige betriebliche Erträge", "ERTRAG", "Buchgewinne aus Verkauf von Anlagevermögen"));
        list.add(new Industriekontenrahmen("5460", "Erträge aus dem Abgang von Gegenständen des Umlaufvermögens", 5, "54 Sonstige betriebliche Erträge", "ERTRAG", "Erträge aus Wertpapierverkäufen UV"));
        list.add(new Industriekontenrahmen("5480", "Erträge aus der Auflösung von Rückstellungen", 5, "54 Sonstige betriebliche Erträge", "ERTRAG", "Auflösung nicht in Anspruch genommener Rückstellungen"));
        list.add(new Industriekontenrahmen("5490", "Periodenfremde Erträge", 5, "54 Sonstige betriebliche Erträge", "ERTRAG", "Erträge aus Vorperioden"));

        // Kontenklasse 6: Betriebliche Aufwendungen
        list.add(new Industriekontenrahmen("6000", "Aufwendungen für Rohstoffe / Fertigungsmaterial", 6, "60 Materialaufwand", "AUFWAND", "Verbrauch von Rohstoffen"));
        list.add(new Industriekontenrahmen("6010", "Aufwendungen für Vorprodukte und Fremdbauteile", 6, "60 Materialaufwand", "AUFWAND", "Verbrauch bezogener Teile"));
        list.add(new Industriekontenrahmen("6020", "Aufwendungen für Hilfsstoffe", 6, "60 Materialaufwand", "AUFWAND", "Verbrauch von Hilfsstoffen"));
        list.add(new Industriekontenrahmen("6030", "Aufwendungen für Betriebsstoffe", 6, "60 Materialaufwand", "AUFWAND", "Verbrauch von Energie und Treibstoffen"));
        list.add(new Industriekontenrahmen("6100", "Fremdleistungen", 6, "61 Fremdleistungen", "AUFWAND", "Lohnfertigung und Fremdleistungen"));
        list.add(new Industriekontenrahmen("6200", "Löhne", 6, "62 Personalaufwand", "AUFWAND", "Fertigungslöhne"));
        list.add(new Industriekontenrahmen("6300", "Gehälter", 6, "63 Personalaufwand", "AUFWAND", "Angestelltengehälter"));
        list.add(new Industriekontenrahmen("6400", "Gesetzliche soziale Aufwendungen", 6, "64 Soziale Abgaben", "AUFWAND", "Arbeitgeberanteil Sozialversicherung"));
        list.add(new Industriekontenrahmen("6420", "Beiträge zur Berufsgenossenschaft", 6, "64 Soziale Abgaben", "AUFWAND", "Unfallversicherung"));
        list.add(new Industriekontenrahmen("6440", "Aufwendungen für Altersversorgung", 6, "64 Soziale Abgaben", "AUFWAND", "Betriebliche Altersversorgung"));
        list.add(new Industriekontenrahmen("6510", "Abschreibungen auf immaterielle Vermögensgegenstände", 6, "65 Abschreibungen", "AUFWAND", "AfA auf immaterielle Werte"));
        list.add(new Industriekontenrahmen("6520", "Abschreibungen auf Sachanlagen", 6, "65 Abschreibungen", "AUFWAND", "Planmäßige AfA auf Sachanlagen"));
        list.add(new Industriekontenrahmen("6540", "Außerplanmäßige Abschreibungen auf Anlagevermögen", 6, "65 Abschreibungen", "AUFWAND", "Außerordentliche Wertminderungen"));
        list.add(new Industriekontenrahmen("6700", "Mietaufwendungen und Pachten", 6, "67 Raumkosten", "AUFWAND", "Miete für Gebäude und Räume"));
        list.add(new Industriekontenrahmen("6730", "Gas, Strom, Wasser, Heizung", 6, "67 Raumkosten", "AUFWAND", "Energiekosten Betriebsräume"));
        list.add(new Industriekontenrahmen("6750", "Instandhaltung und Reparaturen", 6, "67 Instandhaltung", "AUFWAND", "Wartung und Instandsetzung"));
        list.add(new Industriekontenrahmen("6770", "Beiträge, Gebühren, Versicherungen", 6, "67 Versicherungen", "AUFWAND", "Betriebliche Versicherungen"));
        list.add(new Industriekontenrahmen("6800", "Sonstige betriebliche Aufwendungen", 6, "68 Sonstige betriebliche Aufwendungen", "AUFWAND", "Büromaterial, Telefon, Porto"));
        list.add(new Industriekontenrahmen("6820", "Reise- und Bewirtungskosten", 6, "68 Sonstige betriebliche Aufwendungen", "AUFWAND", "Dienstreisen und geschäftliche Bewirtung"));
        list.add(new Industriekontenrahmen("6850", "Werbeaufwendungen", 6, "68 Sonstige betriebliche Aufwendungen", "AUFWAND", "Marketing und Werbekampagnen"));
        list.add(new Industriekontenrahmen("6870", "Beratungs- und Prüfungskosten", 6, "68 Sonstige betriebliche Aufwendungen", "AUFWAND", "Rechts-, Steuerberatung und Wirtschaftsprüfung"));

        // Kontenklasse 7: Weitere Aufwendungen
        list.add(new Industriekontenrahmen("7000", "Betriebliche Steuern", 7, "70 Steuern", "AUFWAND", "Grundsteuer, Kfz-Steuer"));
        list.add(new Industriekontenrahmen("7100", "Zinsaufwendungen und ähnliche Aufwendungen", 7, "71 Finanzergebnis", "AUFWAND", "Kreditzinsen, Bankspesen"));
        list.add(new Industriekontenrahmen("7300", "Erträge aus Beteiligungen", 7, "73 Finanzergebnis", "ERTRAG", "Dividenden und Gewinnausschüttungen"));
        list.add(new Industriekontenrahmen("7400", "Zinserträge und ähnliche Erträge", 7, "74 Finanzergebnis", "ERTRAG", "Bankzinsen und Wertpapierzinsen"));
        list.add(new Industriekontenrahmen("7510", "Abschreibungen auf Finanzanlagen", 7, "75 Finanzergebnis", "AUFWAND", "Wertminderung auf Finanzanlagen"));
        list.add(new Industriekontenrahmen("7600", "Außerordentliche Aufwendungen", 7, "76 Außerordentliches Ergebnis", "AUFWAND", "Katastrophenschäden, Verluste"));
        list.add(new Industriekontenrahmen("7700", "Außerordentliche Erträge", 7, "77 Außerordentliches Ergebnis", "ERTRAG", "Außerordentliche Erträge"));
        list.add(new Industriekontenrahmen("7800", "Steuern vom Einkommen und Ertrag", 7, "78 Ertragsteuern", "AUFWAND", "Körperschaftsteuer, Gewerbesteuer"));

        // Kontenklasse 8: Ergebnisrechnungen
        list.add(new Industriekontenrahmen("8000", "Eröffnungsbilanzkonto (EBK)", 8, "80 Abschlusskonten", "ABSCHLUSS", "Konto zur Eröffnung der Bestandskonten"));
        list.add(new Industriekontenrahmen("8010", "Schlussbilanzkonto (SBK)", 8, "80 Abschlusskonten", "ABSCHLUSS", "Konto zum Abschluss der Bestandskonten"));
        list.add(new Industriekontenrahmen("8020", "Gewinn- und Verlustkonto (GuV)", 8, "80 Abschlusskonten", "ABSCHLUSS", "Abschlusskonto der Erfolgskonten"));

        // Kontenklasse 9: Kosten- und Leistungsrechnung
        list.add(new Industriekontenrahmen("9000", "Kostenartenrechnung", 9, "90 Kosten- und Leistungsrechnung", "KLR", "Erfassung und Abgrenzung der Kostenarten"));
        list.add(new Industriekontenrahmen("9100", "Kostenstellenrechnung", 9, "91 Kostenstellen", "KLR", "Verteilung auf Haupt- und Hilfskostenstellen"));
        list.add(new Industriekontenrahmen("9200", "Kostenträgerrechnung", 9, "92 Kostenträger", "KLR", "Kostenträgerstück- und Zeitrechnung"));
        list.add(new Industriekontenrahmen("9900", "Innerbetriebliche Verrechnung / Abgrenzungen", 9, "99 KLR-Abschluss", "KLR", "Kalkulatorische Kosten und Verrechnung"));

        return list;
    }
}
