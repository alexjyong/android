#!/bin/bash

# script to generate PNG icons from vector drawable
# this converts the vector drawable to PNGs for all required densities

VECTOR_FILE="app/src/main/res/drawable/ic_launcher_foreground.xml"
OUTPUT_BASE="app/src/main/res"

declare -A SIZES=(
    ["mdpi"]=48
    ["hdpi"]=72
    ["xhdpi"]=96
    ["xxhdpi"]=144
    ["xxxhdpi"]=192
)

echo "Generating PNG icons from vector drawable..."

TEMP_SVG="/tmp/temp_icon.svg"

cat > "$TEMP_SVG" << 'EOF'
<svg xmlns="http://www.w3.org/2000/svg" width="108" height="108" viewBox="0 0 108 108">
  <!-- Main R logo -->
  <path d="M42.7,71.38C42.7,60.74 42.7,53.76 42.7,44.7L35.91,36.62H57.87C60.48,36.62 62.77,37.09 64.72,38.03C66.68,38.97 68.2,40.32 69.29,42.08C70.37,43.85 70.92,45.97 70.92,48.43C70.92,50.92 70.36,53.02 69.24,54.73C68.13,56.44 66.56,57.73 64.55,58.6C62.55,59.47 60.21,59.91 57.53,59.91H48.47V52.57H55.61C56.73,52.57 57.69,52.44 58.47,52.17C59.28,51.88 59.89,51.44 60.31,50.83C60.75,50.21 60.96,49.42 60.96,48.43C60.96,47.44 60.75,46.63 60.31,46C59.89,45.37 59.28,44.91 58.47,44.61C57.69,44.31 56.73,44.15 55.61,44.15H52.24V71.38H42.7ZM63.3,55.43L72.09,71.38H61.72L53.14,55.43H63.3Z" fill="#FF005C"/>
  <!-- Small "F" badge in top-right -->
  <circle cx="75" cy="33" r="8" fill="#0066FF"/>
  <path d="M72,29 L72,37 M72,29 L78,29 M72,33 L76,33" stroke="#FFFFFF" stroke-width="1.5" stroke-linecap="round"/>
</svg>
EOF

echo "Created temporary SVG file"

for density in "${!SIZES[@]}"; do
    size=${SIZES[$density]}
    output_dir="$OUTPUT_BASE/mipmap-$density"
    
    echo "Generating ${density} (${size}x${size})..."
    
    rsvg-convert -w $size -h $size "$TEMP_SVG" -o "$output_dir/ic_launcher.png"
    
    # Generate round icon (same image, Android will clip it)
    rsvg-convert -w $size -h $size "$TEMP_SVG" -o "$output_dir/ic_launcher_round.png"
done

rm "$TEMP_SVG"

echo "Done! Generated PNG icons for all densities."
echo "Icons generated in: $OUTPUT_BASE/mipmap-*/"
