package com.vblagoveshchenskii;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SmartlabParser {

    private final static String SITE_URL = "https://smart-lab.ru";
    private final static String PAGE_URL_PART = SITE_URL + "/q/bonds/order_by_val_to_day/desc/page";

    public static void main(String[] args) throws IOException {
        try (CSVPrinter csvPrinter = new CSVPrinter(new FileWriter("output.csv"), CSVFormat.EXCEL)) {
            int currentPageNumber = 1;
            int maxKnownPageNumber = 1;
            while (maxKnownPageNumber >= currentPageNumber) {
                Document document = Jsoup.connect(PAGE_URL_PART + currentPageNumber).get();
                List<Element> rows = document.select("table.trades-table > tbody > tr")
                        .stream()
                        .skip(1)
                        .collect(Collectors.toList());
                for (Element row : rows) {
                    Element link = row.child(2).select("a[href]").get(0);
                    csvPrinter.printRecord(link.ownText(), SITE_URL + link.attr("href"));
                }

                maxKnownPageNumber = Optional.ofNullable(document.getElementById("pagination"))
                        .map(element -> element.getElementsByTag("a"))
                        .map(Elements::stream)
                        .orElseGet(Stream::empty)
                        .map(Element::ownText)
                        .map(SmartlabParser::parseInteger)
                        .filter(OptionalInt::isPresent)
                        .mapToInt(OptionalInt::getAsInt)
                        .max()
                        .orElse(0);
                currentPageNumber++;
            }
        }
    }

    private static OptionalInt parseInteger(String value) {
        try {
            return OptionalInt.of(Integer.parseInt(value));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }
}
