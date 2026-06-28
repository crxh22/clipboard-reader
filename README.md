# Clipboard Reader (nume de lucru: "CitesteMi")

Aplicatie Android care citeste cu voce tare un text — din clipboard sau trimis
din alta aplicatie (text-to-speech).

## Ce face
- Iei un text si aplicatia il citeste cu voce.
- Controale: Play / Pauza / Stop si viteza de citire.
- Merge si cu ecranul stins (notificare cu butoane, ca un player).
- Limba: romana implicit (foloseste vocile de citire instalate pe telefon).
- Fara internet, fara conturi, fara reclame — datele nu pleaca de pe telefon.

## Important: cum citeste din clipboard (o limitare a Android-ului)
Din motive de confidentialitate, Android (versiunea 10 si mai noua) NU lasa o
aplicatie sa citeasca clipboard-ul "in fundal", automat, fara sa o deschizi.
O aplicatie poate citi clipboard-ul doar cand e in prim-plan (deschisa) sau cand
e tastatura activa.

De aceea visul "copiez orice text si imi citeste singur, fara sa ating nimic" NU
e posibil pe Android modern cu o aplicatie obisnuita. Cele mai apropiate moduri
de declansare (toate vor fi incluse) sunt:

1. **Buton in panoul rapid (Quick Settings)** — tragi de sus, apesi o data pe
   "CitesteMi" si citeste ce ai in clipboard. Cel mai aproape de "un singur tap,
   de oriunde".
2. **Selectezi textul -> "Citeste cu voce"** — apare direct in meniul care iese
   cand selectezi un text in orice aplicatie. Cel mai comod cand ai textul in fata.
3. **Share / Distribuie -> CitesteMi** — din orice aplicatie cu buton de share.
4. **Deschizi aplicatia** — citeste automat ce ai in clipboard in acel moment.

## Cum ajunge pe telefon
Nu trebuie instalat nimic greu pe server. La fiecare modificare, un robot pe
GitHub (GitHub Actions) construieste automat aplicatia (fisierul APK) si o pune
la descarcare. O descarci pe telefon din pagina de GitHub si o instalezi (o data
trebuie sa permiti "instalare din surse necunoscute" pentru browser).

## Documente
- `PLAN.md` — planul de lucru pe faze.
- `SPEC.md` — specificatia tehnica (pentru cine construieste).

## Stare
In planificare. Nimic construit inca — astept confirmarea ta pe cele 2-3 decizii
din `PLAN.md` si pornesc Faza 0.
