# NoteMe

NoteMe ist eine native Android-App für den Gedanken, den man kurz festhalten will, während man gerade etwas anderes auf dem Handy macht. Ein kleiner animierter Teddybär schaut am rechten Bildschirmrand hervor. Ein Tipp auf ihn öffnet eine Notizkarte über der aktuellen App. Die Tastatur ist sofort bereit.

## Vom Gedanken zur Notiz

- **Schreiben.** Die Karte speichert den Text beim Tippen lokal auf dem Gerät.
- **Diktieren.** Der Android-Sprachdienst überträgt Gesprochenes in die Notiz. NoteMe bevorzugt die Spracherkennung auf dem Gerät, wenn sie verfügbar ist.
- **Aufräumen.** Im Monatsarchiv lassen sich Notizen nach Datum sortieren, als wichtig markieren oder löschen. Ausgewählte Einträge lassen sich in chronologischer Reihenfolge zu einer Notiz verbinden.

## Ausprobieren

Öffne das Projekt in Android Studio und starte das Modul `app` auf einem Gerät oder Emulator mit Android 8.0 oder neuer. Tippe in NoteMe auf **Bär aktivieren** und erlaube **Über anderen Apps einblenden**. Für das Diktat braucht das Gerät einen Sprachdienst und die App Zugriff auf das Mikrofon.

## Technik

Java, native Android-Views und SQLite. Ein Vordergrunddienst zeichnet den Bären als Android-Overlay. Die Schnellnotiz öffnet sich als Dialog-Activity. NoteMe benötigt kein Konto und keinen API-Schlüssel.
