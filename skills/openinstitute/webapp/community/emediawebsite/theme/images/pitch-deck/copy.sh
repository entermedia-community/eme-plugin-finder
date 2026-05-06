for file in *.png; do convert $file "`basename $file .png`.webp"; done

