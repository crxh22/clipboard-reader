# PLAN DE LUCRU — Clipboard Reader

Plan pe faze. Fiecare faza se termina cu un APK instalabil pe telefon (construit
automat pe GitHub), ca sa poti testa pe bune dupa fiecare pas.

**Stare la zi (v0.1):** Fazele 0-3 sunt in mare facute si livrate in primul build:
auto-detectie ro/ru, "Citeste cu voce" (Chrome), share, dala in panoul rapid,
serviciu de citire cu notificare, buton flotant (Facebook), ecran cu viteza +
limba. Compileaza curat, 8 teste unitare trec. Urmeaza testul pe telefonul tau.

---

## Faza 0 — Schelet + lant de build (infra)
- Structura proiectului Android (Gradle, Kotlin, Compose), wrapper-ul Gradle.
- GitHub Actions: build automat al APK-ului la fiecare modificare -> fisier
  descarcabil.
- **Rezultat:** un APK "gol" instalabil. Dovada ca lantul construire -> descarcare
  -> instalare pe telefonul tau merge, inainte sa punem functii in el.

## Faza 1 — Nucleul de citire (MVP)
- Primire text prin Share si prin "Selectezi text -> Citeste cu voce".
- Motorul de citire (text-to-speech), limba romana.
- Ecran simplu: textul + Play / Pauza / Stop + viteza.
- **Rezultat:** selectezi sau dai share la un text si il auzi citit. Utilizabil.

## Faza 2 — Clipboard cu un tap
- Buton in panoul rapid (Quick Settings) care citeste clipboard-ul.
- Citire automata a clipboard-ului cand deschizi aplicatia.
- **Rezultat:** tragi de sus, un tap, si iti citeste ce ai copiat.

## Faza 3 — Player adevarat (texte lungi, ecran stins)
- Serviciu in fundal + notificare cu Play/Pauza/Stop (merge cu ecranul stins si
  pe ecranul de blocare).
- Se opreste politicos cand suna telefonul sau cand alta aplicatie reda sunet.
- Imparte textele lungi pe propozitii (peste limita motorului de citire).
- **Rezultat:** citeste si articole lungi, controlabil din notificare.

## Faza 4 — Rafinari (optional, dupa nevoie)
- Detectie automata a limbii / alegerea vocii / viteza si ton in setari.
- Istoric sau coada de citire.
- Export in fisier audio (MP3/WAV).
- Buton flotant pe ecran (overlay).
- Icon si nume final, finisaje.

---

## Distributie
La fiecare faza, APK-ul nou apare automat pe GitHub si il instalezi pe telefon.
Prima data permiti "instalare din surse necunoscute" pentru browser; apoi doar
descarci si apesi.

## Efort (orientativ)
- Faza 0 + 1 = nucleul utilizabil (cel mai important).
- Faza 2 + 3 = experienta completa de zi cu zi.
- Faza 4 = optional, alegi tu ce merita.

---

## Decizii (rezolvate 28-06)

1. **Declansare:** Chrome → "Citeste cu voce" din meniul de selectie; Facebook →
   buton flotant dupa Copy (plus dala in panoul rapid); share din orice aplicatie.
2. **Limba:** romana + rusa, alese **automat** dupa scris (chirilica → rusa),
   cu fixare manuala optionala.
3. **Build:** schelet + GitHub Actions = gata; in plus, build local complet pe
   server (compileaza + teste unitare).

## Ce urmeaza
- Testul pe telefonul tau (vocea, dala, butonul flotant) — singurul lucru care nu
  se poate verifica fara un telefon real.
- Semnatura stabila a APK-ului, ca update-urile sa se instaleze peste cel vechi
  fara dezinstalare (acum primul build e semnat de test).
- Optional, daca Facebook ascunde meniul de selectie: varianta cu serviciu de
  accesibilitate (citeste textul selectat direct, fara Copy).
- Icon si nume final, finisaje.
