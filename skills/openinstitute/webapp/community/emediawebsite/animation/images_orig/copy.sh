mkdir -p ../images/folders
for file in *.png; do convert $file "../images/`basename $file .png`.webp"; done

cd folders
for file in *.png; do convert $file "../../images/folders/`basename $file .png`.webp"; done
