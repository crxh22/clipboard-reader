# Clipboard Reader (CitesteMi)

Aplicatie Android care citeste cu voce tare un text — selectat, copiat sau trimis
din alta aplicatie. Romana + rusa, cu alegerea automata a limbii.

## Stare: v0.1 (primul build de test)
Construita si verificata local: compileaza curat + 8 teste unitare trec.
Citirea efectiva (vocea, dala din panoul rapid, butonul flotant) se testeaza pe
telefon — vezi raportul / instructiunile de mai jos.

## Cum folosesti
- **Chrome:** selecteaza textul → **"Citeste cu voce"** din meniul de selectie (1 tap).
- **Facebook:** copiaza textul → apasa **butonul flotant** (sau dala **"Citeste clipboard"** din panoul rapid).
- **Orice aplicatie:** Distribuie / Share → **CitesteMi**.
- **In aplicatie:** butonul **"Citeste din clipboard acum"**.

Limba se alege **automat**: text in chirilica → rusa, altfel romana. Se poate
fixa manual pe romana sau rusa din ecranul aplicatiei. Viteza e reglabila.

## Cum o instalezi pe telefon
1. Descarca `CitesteMi-debug.apk` din pagina **Releases** a proiectului.
2. Deschide fisierul descarcat; Android iti cere sa permiti "instalare din surse
   necunoscute" pentru browser — accepta o singura data.
3. Deschide CitesteMi o data: accepta notificarile. Daca vrei butonul flotant
   (pentru Facebook), porneste-l din ecran — va cere permisiunea
   "afisare peste alte aplicatii".
4. Daca lipsesc vocile romana/rusa, apasa **"Instaleaza vocile"** (te duce la
   setarile de text-to-speech ale telefonului).

## De ce nu citeste 100% automat in fundal (limitare Android)
Din Android 10, o aplicatie poate citi clipboard-ul **doar cand e in prim-plan**
(deschisa) sau cand e tastatura activa — nu in fundal. De aceea declansarea se
face cu un tap: din meniul de selectie, din butonul flotant, din dala panoului
rapid sau din share. Textul selectat / dat la share ajunge direct, fara clipboard.

## Documente
- `PLAN.md` — planul pe faze si ce urmeaza.
- `SPEC.md` — specificatia tehnica (stack, declansare, TTS, build/distributie).

## Build (pentru dezvoltare)
`./gradlew assembleDebug` → `app/build/outputs/apk/debug/app-debug.apk`.
La fiecare push pe `main`, GitHub Actions construieste APK-ul; un tag `v*`
il ataseaza automat la un Release.
