import os
from PIL import Image

def create_icons(src_path, res_dir):
    sizes = {
        "mipmap-mdpi": 48,
        "mipmap-hdpi": 72,
        "mipmap-xhdpi": 96,
        "mipmap-xxhdpi": 144,
        "mipmap-xxxhdpi": 192,
    }

    try:
        img = Image.open(src_path)
    except Exception as e:
        print(f"Error opening image: {e}")
        return

    # Delete existing ic_launcher files (both png and xml)
    for folder in list(sizes.keys()) + ["mipmap-anydpi-v26", "mipmap-anydpi-v33"]:
        d = os.path.join(res_dir, folder)
        if os.path.exists(d):
            print(f"Cleaning {d}")
            for file in os.listdir(d):
                if file.startswith("ic_launcher") and (file.endswith(".png") or file.endswith(".webp") or file.endswith(".xml")):
                    try:
                        os.remove(os.path.join(d, file))
                    except:
                        pass

    for folder, size in sizes.items():
        folder_path = os.path.join(res_dir, folder)
        os.makedirs(folder_path, exist_ok=True)
        
        resized = img.resize((size, size), Image.Resampling.LANCZOS)
        
        # Save round version
        resized.save(os.path.join(folder_path, "ic_launcher_round.png"))
        # Save standard version
        resized.save(os.path.join(folder_path, "ic_launcher.png"))

if __name__ == "__main__":
    src = r"C:\Users\nikhi\.gemini\antigravity\brain\3ca29332-93e5-4486-8c87-1e91e7e19747\app_icon.png"
    res = r"d:\Vertical_rotatory_parking_system\app\src\main\res"
    create_icons(src, res)
    print("Icons generated successfully!")
