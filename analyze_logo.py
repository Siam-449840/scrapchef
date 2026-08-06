from PIL import Image
import numpy as np

img = Image.open('app/ScrapChef-logo.png').convert('RGB')
data = np.array(img)

# Find pixels that are NOT white/grey (i.e. have high saturation/color)
# Convert to HSV to easily threshold saturation
import matplotlib.colors as mcolors
hsv = mcolors.rgb_to_hsv(data / 255.0)

# Saturation is hsv[:, :, 1]
# Let's see max/min saturation
sat = hsv[:, :, 1]
val = hsv[:, :, 2]

# Pixels with saturation > 0.2 and value > 0.2 are likely the orange logo
mask = (sat > 0.15) & (val > 0.2)

# Find bounding box
rows = np.any(mask, axis=1)
cols = np.any(mask, axis=0)

if not np.any(rows):
    print("No colored pixels found!")
else:
    rmin, rmax = np.where(rows)[0][[0, -1]]
    cmin, cmax = np.where(cols)[0][[0, -1]]
    print(f"Bounding box: {cmin}, {rmin}, {cmax}, {rmax}")
    print(f"Original size: {img.width}x{img.height}")
    
    # Save a diagnostic image where mask is applied
    rgba = np.zeros((data.shape[0], data.shape[1], 4), dtype=np.uint8)
    rgba[:, :, :3] = data
    rgba[:, :, 3] = mask * 255
    
    # Crop to bounding box with small padding
    pad = 10
    rmin = max(0, rmin - pad)
    rmax = min(img.height, rmax + pad)
    cmin = max(0, cmin - pad)
    cmax = min(img.width, cmax + pad)
    
    cropped = rgba[rmin:rmax, cmin:cmax]
    out_img = Image.fromarray(cropped)
    out_img.save('app/src/main/res/drawable/ic_scrapchef_logo.png')
    print("Saved transparent cropped logo!")
