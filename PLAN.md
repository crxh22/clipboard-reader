# PLAN DE LUCRU — Clipboard Reader

Plan pe faze. Fiecare faza se termina cu un APK instalabil pe telefon (construit
automat pe GitHub), ca sa poti testa pe bune dupa fiecare pas.

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

## Decizii (am nevoie de confirmarea ta — am si recomandare la fiecare)

1. **Modul de declansare din clipboard.** Android nu lasa citire automata in
   fundal (vezi README). Recomandarea mea: buton in panoul rapid (un tap) +
   "selectezi text -> Citeste cu voce" + share. **OK asa, sau te asteptai la
   altceva?**

2. **Limba.** Recomand: romana implicit la inceput, iar detectia automata a
   limbii o adaugam in Faza 4. **Citesti mai ales in romana, sau ai nevoie si de
   engleza/alte limbi de la inceput?**

3. **Pornesc Faza 0 acum?** Adica fac scheletul + build-ul automat pe GitHub, ca
   sa ai repede un APK de probat. Recomand: da.

Numele aplicatiei si icon-ul le putem alege pe parcurs (nu blocheaza nimic).
