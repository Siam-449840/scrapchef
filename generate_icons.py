import os
from PIL import Image, ImageDraw

def create_circular_mask(size):
    mask = Image.new('L', size, 0)
    draw = ImageDraw.Draw(mask)
    draw.ellipse((0, 0) + size, fill=255)
    return mask

def generate_icons():
    src_img = Image.open('app/ScrapChef-logo.png').convert("RGBA")
    # For legacy icon, maybe it looks best without a background or with the background?
    # Adaptive icons will use the foreground on top of the XML background.
    # We will assume the logo provided is the foreground.
    
    # Let's see if we should create a background-colored legacy icon.
    bg_color = (248, 250, 252) # close to the gradient center

    densities = {
        'mdpi': (48, 108),
        'hdpi': (72, 162),
        'xhdpi': (96, 216),
        'xxhdpi': (144, 324),
        'xxxhdpi': (192, 432)
    }

    base_res_dir = 'app/src/main/res'
    
    for density, (legacy_size, adaptive_size) in densities.items():
        dir_path = os.path.join(base_res_dir, f'mipmap-{density}')
        os.makedirs(dir_path, exist_ok=True)
        
        # 1. Adaptive foreground
        # Resize to adaptive size
        fg_img = src_img.resize((adaptive_size, adaptive_size), Image.Resampling.LANCZOS)
        fg_img.save(os.path.join(dir_path, 'ic_launcher_foreground.png'))
        
        # 2. Legacy square
        legacy_img = Image.new("RGBA", (legacy_size, legacy_size), bg_color)
        fg_resized_for_legacy = src_img.resize((int(legacy_size * 0.7), int(legacy_size * 0.7)), Image.Resampling.LANCZOS)
        offset = ((legacy_size - fg_resized_for_legacy.width) // 2, (legacy_size - fg_resized_for_legacy.height) // 2)
        legacy_img.paste(fg_resized_for_legacy, offset, fg_resized_for_legacy)
        legacy_img.save(os.path.join(dir_path, 'ic_launcher.png'))

        # 3. Legacy round
        round_img = legacy_img.copy()
        mask = create_circular_mask((legacy_size, legacy_size))
        round_img.putalpha(mask)
        round_img.save(os.path.join(dir_path, 'ic_launcher_round.png'))

        # Remove old webp files to avoid conflicts
        for old_file in ['ic_launcher.webp', 'ic_launcher_round.webp', 'ic_launcher_foreground.webp']:
            old_path = os.path.join(dir_path, old_file)
            if os.path.exists(old_path):
                os.remove(old_path)
                
generate_icons()
