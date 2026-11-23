/*
 * SPDX-License-Identifier: EUPL-1.2 OR LicenseRef-commercial
 *
 * Copyright (c) 2012-2026 mgm technology partners GmbH
 *
 * Dual License
 * ------------
 * This source file is part of the mgm A12 Platform and available under
 * a choice of two different licenses:
 *
 * 1. Open-Source License – EUPL v1.2
 *    You may redistribute and/or modify this file under the terms of the
 *    European Union Public License, version 1.2 - see https://eupl.eu/.
 *
 * 2. Commercial License
 *    Alternatively, you may obtain a commercial license from
 *    mgm technology partners GmbH, that permits use of this software
 *    under different terms (including support and maintenance services).
 *
 *    Please contact a12-license@mgm-tp.com for more information.
 *
 * You must select and comply with exactly one of the above license options.
 *
 * Warranty Disclaimer (applies to either option)
 * ----------------------------------------------
 * THIS SOFTWARE IS PROVIDED “AS IS” AND WITHOUT WARRANTY OF ANY KIND,
 * WHETHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NON-INFRINGEMENT, EXCEPT WHERE SUCH DISCLAIMERS ARE HELD TO BE
 * LEGALLY INVALID. SEE THE RESPECTIVE LICENSE TEXT FOR DETAILS.
 */
package com.mgmtp.a12.kernel.core.parser.internal.grammar.languages;

/**
 *
 */

import java.util.ListResourceBundle;

import com.mgmtp.a12.kernel.core.parser.internal.grammar.gen.Base_ConditionLexer;

public class LexerTerminals_de extends ListResourceBundle {

	private static final Object[][] CONTENTS = new Object[][] {
			{ "Abrunden", Base_ConditionLexer.ABRUNDEN },
			{ "AbrundenWert", Base_ConditionLexer.ABRUNDEN_WERT },
			{ "Abs", Base_ConditionLexer.ABS },
			{ "AbsWert", Base_ConditionLexer.ABS_WERT },
			{ "AddiereJahre", Base_ConditionLexer.ADDIERE_JAHRE },
			{ "AddiereMinuten", Base_ConditionLexer.ADDIERE_MINUTEN },
			{ "AddiereMonate", Base_ConditionLexer.ADDIERE_MONATE },
			{ "AddiereSekunden", Base_ConditionLexer.ADDIERE_SEKUNDEN },
			{ "AddiereStunden", Base_ConditionLexer.ADDIERE_STUNDEN },
			{ "AddiereTage", Base_ConditionLexer.ADDIERE_TAGE },
			{ "AktuelleVordruckNummer", Base_ConditionLexer.AKTUELLE_VORDRUCK_NUMMER },
			{ "AktuelleWiederholung", Base_ConditionLexer.AKTUELLE_WIEDERHOLUNG },
			{ "AlleFelderAngegeben", Base_ConditionLexer.ALLE_FELDER_ANGEGEBEN },
			{ "AlleKontexteAngegeben", Base_ConditionLexer.ALLE_KONTEXTE_ANGEGEBEN },
			{ "AnzahlAngegebenerFelder", Base_ConditionLexer.ANZAHL_ANGEGEBENER_FELDER },
			{ "AnzahlAngegebenerKontexte", Base_ConditionLexer.ANZAHL_ANGEGEBENER_KONTEXTE },
			{ "AnzahlAngegebenerVordrucke", Base_ConditionLexer.ANZAHL_ANGEGEBENER_VORDRUCKE },
			{ "AnzahlAngegebeneUsb", Base_ConditionLexer.ANZAHL_ANGEGEBENE_USB },
			{ "AnzahlVerschiedenerWerte", Base_ConditionLexer.ANZAHL_VERSCHIEDENER_WERTE },
			{ "AnzahlWertInFeldliste", Base_ConditionLexer.ANZAHL_WERT_IN_FELDLISTE },
			{ "ApplikationsBedingung", Base_ConditionLexer.APPLIKATIONS_BEDINGUNG },
			{ "Aufrunden", Base_ConditionLexer.AUFRUNDEN },
			{ "AufrundenWert", Base_ConditionLexer.AUFRUNDEN_WERT },
			{ "BereichAlsString", Base_ConditionLexer.BEREICH_ALS_STRING },
			{ "BereichAlsZahl", Base_ConditionLexer.BEREICH_ALS_ZAHL },
			{ "DateTime", Base_ConditionLexer.DATETIME },
			{ "Datum", Base_ConditionLexer.DATUM },
			{ "DatumAusDateTime", Base_ConditionLexer.DATUM_AUS_DATETIME },
			{ "DatumBereich", Base_ConditionLexer.DATUM_BEREICH },
			{ "DatumsBereicheUeberlappend", Base_ConditionLexer.DATUMS_BEREICHE_UEBERLAPPEND },
			{ "DifferenzInJahren", Base_ConditionLexer.DIFFERENZ_IN_JAHREN },
			{ "DifferenzInMinuten", Base_ConditionLexer.DIFFERENZ_IN_MINUTEN },
			{ "DifferenzInMonaten", Base_ConditionLexer.DIFFERENZ_IN_MONATEN },
			{ "DifferenzInSekunden", Base_ConditionLexer.DIFFERENZ_IN_SEKUNDEN },
			{ "DifferenzInStunden", Base_ConditionLexer.DIFFERENZ_IN_STUNDEN },
			{ "DifferenzInTagen", Base_ConditionLexer.DIFFERENZ_IN_TAGEN },
			{ "EndeAusDatumBereich", Base_ConditionLexer.ENDE_AUS_DATUM_BEREICH },
			{ "ErsterTag", Base_ConditionLexer.ERSTER_TAG },
			{ "ErsterAngegebenerWert", Base_ConditionLexer.ERSTER_ANGEGEBENER_WERT },
			{ "Falsch", Base_ConditionLexer.FALSCH },
			{ "FeldAngegeben", Base_ConditionLexer.FELD_ANGEGEBEN },
			{ "FelderNichtGemeinsamAngegeben", Base_ConditionLexer.FELDER_NICHT_GEMEINSAM_ANGEGEBEN },
			{ "FeldNichtAngegeben", Base_ConditionLexer.FELD_NICHT_ANGEGEBEN },
			{ "FeldWertAlsString", Base_ConditionLexer.FELD_WERT_ALS_STRING },
			{ "FeldWertAlsZahl", Base_ConditionLexer.FELD_WERT_ALS_ZAHL },
			{ "FeldWerteNichtEindeutig", Base_ConditionLexer.FELD_WERTE_NICHT_EINDEUTIG },
			{ "FeldWertInWerteListe", Base_ConditionLexer.FELD_WERT_IN_WERTE_LISTE },
			{ "FeldWertNichtInWerteListe", Base_ConditionLexer.FELD_WERT_NICHT_IN_WERTE_LISTE },
			{ "Fuer", Base_ConditionLexer.FOR },
			{ "Gueltig", Base_ConditionLexer.GUELTIG },
			{ "Heute", Base_ConditionLexer.HEUTE },
			{ "In", Base_ConditionLexer.IN1 },
			{ "in", Base_ConditionLexer.IN2 },
			{ "IN", Base_ConditionLexer.IN3 },
			{ "JahrAusDatum", Base_ConditionLexer.JAHR_AUS_DATUM },
			{ "Jetzt", Base_ConditionLexer.JETZT },
			{ "KeinFeldAngegeben", Base_ConditionLexer.KEIN_FELD_ANGEGEBEN },
			{ "KeinFeldWertInWerteListe", Base_ConditionLexer.KEIN_FELD_WERT_IN_WERTE_LISTE },
			{ "KeinKontextAngegeben", Base_ConditionLexer.KEIN_KONTEXT_ANGEGEBEN },
			{ "KeinVordruckAngegeben", Base_ConditionLexer.KEIN_VORDRUCK_ANGEGEBEN },
			{ "KontextAngegeben", Base_ConditionLexer.KONTEXT_ANGEGEBEN },
			{ "KontexteNichtGemeinsamAngegeben", Base_ConditionLexer.KONTEXTE_NICHT_GEMEINSAM_ANGEGEBEN },
			{ "KontextNichtAngegeben", Base_ConditionLexer.KONTEXT_NICHT_ANGEGEBEN },
			{ "Laenge", Base_ConditionLexer.LAENGE },
			{ "LetzterTag", Base_ConditionLexer.LETZTER_TAG },
			{ "Max", Base_ConditionLexer.MAX },
			{ "MaximalAngegebenerZeilenindex", Base_ConditionLexer.MAXIMAL_ANGEGEBENER_ZEILENINDEX },
			{ "MaxWert", Base_ConditionLexer.MAX_WERT },
			{ "MehrAlsEinFeldAngegeben", Base_ConditionLexer.MEHR_ALS_EIN_FELD_ANGEGEBEN },
			{ "Min", Base_ConditionLexer.MIN },
			{ "MindestensEinDatumsBereichUeberlappend", Base_ConditionLexer.MINDESTENS_EIN_DATUMSBEREICH_UEBERLAPPEND },
			{ "MindestensEinFeldAngegeben", Base_ConditionLexer.MINDESTENS_EIN_FELD_ANGEGEBEN },
			{ "MindestensEinFeldWertInWerteListe", Base_ConditionLexer.MINDESTENS_EIN_FELD_WERT_IN_WERTE_LISTE },
			{ "MindestensEinKontextAngegeben", Base_ConditionLexer.MINDESTENS_EIN_KONTEXT_ANGEGEBEN },
			{ "MindestensEinVordruckAngegeben", Base_ConditionLexer.MINDESTENS_EIN_VORDRUCK_ANGEGEBEN },
			{ "MinutenAusZeit", Base_ConditionLexer.MINUTEN_AUS_ZEIT },
			{ "MinWert", Base_ConditionLexer.MIN_WERT },
			{ "Mit", Base_ConditionLexer.MIT },
			{ "MonatAusDatum", Base_ConditionLexer.MONAT_AUS_DATUM },
			{ "NichtAlleFelderOderKeinFeldAngegeben", Base_ConditionLexer.NICHT_ALLE_FELDER_ODER_KEIN_FELD_ANGEGEBEN },
			{ "NichtAlleFeldWerteInWerteListe", Base_ConditionLexer.NICHT_ALLE_FELD_WERTE_IN_WERTE_LISTE },
			{ "NichtAlleOderKeinKontextAngegeben", Base_ConditionLexer.NICHT_ALLE_ODER_KEIN_KONTEXT_ANGEGEBEN },
			{ "NichtGenauEinFeldAngegeben", Base_ConditionLexer.NICHT_GENAU_EIN_FELD_ANGEGEBEN },
			{ "NichtWieMuster", Base_ConditionLexer.NICHT_WIE_MUSTER },
			{ "Oder", Base_ConditionLexer.ODER1 },
			{ "oder", Base_ConditionLexer.ODER2 },
			{ "ODER", Base_ConditionLexer.ODER3 },
			{ "QuartalAusDatum", Base_ConditionLexer.QUARTAL_AUS_DATUM },
			{ "Runden", Base_ConditionLexer.RUNDEN },
			{ "RundenWert", Base_ConditionLexer.RUNDEN_WERT },
			{ "RegelKontext", Base_ConditionLexer.REGEL_KONTEXT },
			{ "SekundenAusZeit", Base_ConditionLexer.SEKUNDEN_AUS_ZEIT },
			{ "StartAusDatumBereich", Base_ConditionLexer.START_AUS_DATUM_BEREICH },
			{ "StundenAusZeit", Base_ConditionLexer.STUNDEN_AUS_ZEIT },
			{ "Summe", Base_ConditionLexer.SUMME },
			{ "SummeVonProdukten", Base_ConditionLexer.SUMME_VON_PRODUKTEN },
			{ "TagAusDatum", Base_ConditionLexer.TAG_AUS_DATUM },
			{ "Und", Base_ConditionLexer.UND1 },
			{ "und", Base_ConditionLexer.UND2 },
			{ "UND", Base_ConditionLexer.UND3 },
			{ "UngleichMitToleranz1", Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ1 },
			{ "UngleichMitToleranz2", Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ2 },
			{ "UngleichMitToleranz5", Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ5 },
			{ "UngleichMitToleranz10", Base_ConditionLexer.UNGLEICH_MIT_TOLERANZ10 },
			{ "UnterdrueckeWarnung", Base_ConditionLexer.UNTERDRUECKE_WARNUNG },
			{ "Ungueltig", Base_ConditionLexer.UNGUELTIG },
			{ "UsbNichtFortlaufend", Base_ConditionLexer.USB_NICHT_FORTLAUFEND },
			{ "Referenzjahr", Base_ConditionLexer.REFERENZJAHR },
			{ "@Von", Base_ConditionLexer.VON },
			{ "VordruckAngegeben", Base_ConditionLexer.VORDRUCK_ANGEGEBEN },
			{ "VordruckNichtAngegeben", Base_ConditionLexer.VORDRUCK_NICHT_ANGEGEBEN },
			{ "VordruckNichtFortlaufend", Base_ConditionLexer.VORDRUCK_NICHT_FORTLAUFEND },
			{ "Wahr", Base_ConditionLexer.WAHR },
			{ "WertAlsDatum", Base_ConditionLexer.WERT_ALS_DATUM },
			{ "WertNichtKonsistent", Base_ConditionLexer.WERT_NICHT_KONSISTENT },
			{ "WiederholungNichtEindeutig", Base_ConditionLexer.WIEDERHOLUNG_NICHT_EINDEUTIG },
			{ "WieMuster", Base_ConditionLexer.WIE_MUSTER },
			{ "ZeileNichtEindeutig", Base_ConditionLexer.ZEILE_NICHT_EINDEUTIG },
			{ "ZeilenLueckenlos", Base_ConditionLexer.ZEILEN_LUECKENLOS },
			{ "ZeilenNichtAufsteigend", Base_ConditionLexer.ZEILEN_NICHT_AUFSTEIGEND },
			{ "Zeit", Base_ConditionLexer.TIME },
			{ "ZeitAusDateTime", Base_ConditionLexer.ZEIT_AUS_DATETIME },
			{ "zu", Base_ConditionLexer.ZU1 },
			{ "Zu", Base_ConditionLexer.ZU2 },
			{ "ZU", Base_ConditionLexer.ZU3 },
	};

	@Override
	protected Object[][] getContents() {
		return CONTENTS;
	}

}
