# Enhanced features README

This archive contains the enhancements branch for DSE_FINAL. It includes:

- ImportService: CSV import templates and a generic CSV importer for items, parties, sales, purchase and quotation-like headers.
- WhatsappService: helper to open wa.me links and open the folder containing generated PDFs so the user can attach them.
- ReminderService: simple in-app scheduler that loads reminders from the database (reminders table) and schedules them.
- NotificationService: helper to create notifications in the notifications table.
- DB migration SQL: sql/migrations/V1__add_reminders_and_notifications.sql
- Import templates: src/main/resources/import_templates/
- config.properties.template: new template with payment and SMTP placeholders.

How to apply DB migration
1. Ensure the application is stopped.
2. Open your JavaAppERP.db (SQLite) using the SQLite CLI or DB Browser for SQLite.
3. Run the SQL in sql/migrations/V1__add_reminders_and_notifications.sql to create the reminders and notifications tables.

How to build
- From the project root run:

mvn clean package

This will compile the project. The fat/shaded jar is not included in this ZIP by request.

How to test key features
- Import: use the CSV templates in src/main/resources/import_templates/ and use ImportService.importItems/ importParties/ importSales etc. via a small test harness or by wiring to the UI.
- WhatsApp: call WhatsappService.openWhatsappWithMessage(phone, message, pdfPath) to open wa.me link and the folder containing the PDF.
- Reminders: after running the migration, insert a row into reminders table with trigger_time as epoch millis or ISO-8601 string and method 'EMAIL' or 'WHATSAPP'. Restart the app and invoke ReminderService.start(handler) with an appropriate handler that sends email or builds wa.me URL.
- Notifications: use NotificationService.createNotification(...) to create notifications.

Next steps (planned but not finished in this ZIP)
- UI integration: controllers and FXML wiring to expose the import dialogs, filter popovers, reminders and quotation screens in the app UI.
- More robust CSV parsing (multi-line invoice lines) and bulk transaction handling for imports.
- Automated tests and improved error reporting for import.

If you want, I can now:
- continue and wire these services into the UI controllers and FXML pages so features are fully accessible from the app.
- produce a downloadable ZIP and upload it to a place you can download (Option B).

