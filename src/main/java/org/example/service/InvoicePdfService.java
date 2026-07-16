package org.example.service;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

import org.example.config.ConfigManager;
import org.example.model.*;

import java.awt.Color;
import java.io.IOException;
import java.nio.file.*;

public final class InvoicePdfService {


    private static final PDType1Font BOLD =
        new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);


    private static final PDType1Font REGULAR =
        new PDType1Font(Standard14Fonts.FontName.HELVETICA);



    private InvoicePdfService() {

    }



    //====================================================
    // PURCHASE PDF
    //====================================================

    public static Path purchase(Purchase p)
        throws IOException {


        return create(

            p.getInvoiceNo(),

            p.getInvoiceDate().toString(),

            "Purchase Invoice",

            p.getSupplier(),

            p.getLines(),

            p.getSubtotal(),

            p.getGstAmount(),

            p.getTotalAmount()

        );

    }





    //====================================================
    // SALES PDF
    //====================================================

    public static Path sale(Sales s)
        throws IOException {


        return create(

            s.getInvoiceNo(),

            s.getInvoiceDate().toString(),

            "Tax Invoice",

            s.getCustomer(),

            s.getLines(),

            s.getSubtotal(),

            s.getGstAmount(),

            s.getTotalAmount()

        );

    }





    //====================================================
    // CREATE PDF
    //====================================================

    private static Path create(

        String number,

        String date,

        String title,

        Party party,

        java.util.List<? extends InvoiceLine> lines,

        double subtotal,

        double gst,

        double total


    ) throws IOException {



        Path file =
            Files.createTempFile(
                "javaapp-" + number + "-",
                ".pdf"
            );



        try(PDDocument document =
                new PDDocument()) {



            PDPage page =
                new PDPage(
                    PDRectangle.A4
                );


            document.addPage(page);



            try(PDPageContentStream c =
                    new PDPageContentStream(
                        document,
                        page
                    )) {



                //----------------------------------------
                // Header
                //----------------------------------------

                c.setNonStrokingColor(
                    new Color(20,54,92)
                );


                c.addRect(
                    0,
                    760,
                    595,
                    82
                );


                c.fill();



                text(

                    c,
                    BOLD,
                    20,
                    Color.WHITE,
                    42,
                    800,
                    ConfigManager.get(
                        "company.name",
                        "JavaApp ERP"
                    )

                );



                text(

                    c,
                    REGULAR,
                    10,
                    Color.WHITE,
                    42,
                    781,
                    title + " | " + number

                );




                //----------------------------------------
                // Party
                //----------------------------------------

                text(

                    c,
                    BOLD,
                    11,
                    new Color(20,54,92),
                    42,
                    735,
                    "Bill To / From"

                );



                text(

                    c,
                    REGULAR,
                    10,
                    Color.DARK_GRAY,
                    42,
                    717,
                    party.getName()

                );



                text(

                    c,
                    REGULAR,
                    9,
                    Color.DARK_GRAY,
                    42,
                    702,
                    "Email: "
                        + safe(party.getEmail())
                        + " | Date: "
                        + date

                );




                //----------------------------------------
                // Table Header
                //----------------------------------------

                float y = 665;


                c.setNonStrokingColor(
                    new Color(56,92,137)
                );


                c.addRect(
                    42,
                    y-14,
                    511,
                    18
                );


                c.fill();



                String[] headers =
                    {
                        "Item",
                        "Qty",
                        "Rate",
                        "GST %",
                        "Amount"
                    };


                float[] x =
                    {
                        46,
                        310,
                        360,
                        425,
                        485
                    };



                for(int i=0;i<headers.length;i++){


                    text(

                        c,
                        BOLD,
                        8,
                        Color.WHITE,
                        x[i],
                        y-9,
                        headers[i]

                    );

                }




                //----------------------------------------
                // Lines
                //----------------------------------------

                y -=31;



                for(InvoiceLine line : lines){


                    text(
                        c,
                        REGULAR,
                        8,
                        Color.DARK_GRAY,
                        46,
                        y,
                        clip(
                            line.getItemDescription(),
                            42
                        )
                    );


                    text(
                        c,
                        REGULAR,
                        8,
                        Color.DARK_GRAY,
                        310,
                        y,
                        String.format(
                            "%.2f",
                            line.getQuantity()
                        )
                    );


                    text(
                        c,
                        REGULAR,
                        8,
                        Color.DARK_GRAY,
                        360,
                        y,
                        String.format(
                            "Rs. %,.2f",
                            line.getRate()
                        )
                    );


                    text(
                        c,
                        REGULAR,
                        8,
                        Color.DARK_GRAY,
                        425,
                        y,
                        String.format(
                            "%.2f",
                            line.getGstPercent()
                        )
                    );


                    text(
                        c,
                        REGULAR,
                        8,
                        Color.DARK_GRAY,
                        485,
                        y,
                        String.format(
                            "Rs. %,.2f",
                            line.getLineTotal()
                        )
                    );


                    y -=17;

                }





                //----------------------------------------
                // Totals
                //----------------------------------------

                y -=14;



                text(
                    c,
                    REGULAR,
                    10,
                    Color.DARK_GRAY,
                    390,
                    y,
                    "Subtotal"
                );


                text(
                    c,
                    REGULAR,
                    10,
                    Color.DARK_GRAY,
                    485,
                    y,
                    String.format(
                        "Rs. %,.2f",
                        subtotal
                    )
                );



                y -=17;



                text(
                    c,
                    REGULAR,
                    10,
                    Color.DARK_GRAY,
                    390,
                    y,
                    "GST"
                );


                text(
                    c,
                    REGULAR,
                    10,
                    Color.DARK_GRAY,
                    485,
                    y,
                    String.format(
                        "Rs. %,.2f",
                        gst
                    )
                );



                y -=20;



                text(
                    c,
                    BOLD,
                    12,
                    new Color(20,54,92),
                    390,
                    y,
                    "Grand Total"
                );


                text(
                    c,
                    BOLD,
                    12,
                    new Color(20,54,92),
                    485,
                    y,
                    String.format(
                        "Rs. %,.2f",
                        total
                    )
                );



                text(
                    c,
                    REGULAR,
                    8,
                    Color.GRAY,
                    42,
                    35,
                    "Generated by JavaApp ERP | Thank you for your business."
                );


            }


            document.save(
                file.toFile()
            );

        }


        return file;

    }





    private static void text(

        PDPageContentStream c,

        PDType1Font font,

        float size,

        Color color,

        float x,

        float y,

        String value

    ) throws IOException {


        c.beginText();

        c.setFont(
            font,
            size
        );


        c.setNonStrokingColor(
            color
        );


        c.newLineAtOffset(
            x,
            y
        );


        c.showText(
            safe(value)
                .replaceAll(
                    "[^\\x20-\\x7E]",
                    "?"
                )
        );


        c.endText();

    }





    private static String safe(String value){

        return value == null
            ? ""
            : value;

    }





    private static String clip(
        String value,
        int length
    ){

        value = safe(value);


        return value.length() > length

            ? value.substring(0,length-3)+"..."

            : value;

    }
    //====================================================
// SALES PDF (Alias)
//====================================================

    public static Path sales(Sales s)
        throws IOException {

        return sale(s);

    }


}
