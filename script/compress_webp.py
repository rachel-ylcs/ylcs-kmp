import os
from PIL import Image
import glob

def compress_webp_images():
    """
    压缩所有子目录中的webp图片
    将压缩后的图片保存在相同位置，文件名在后缀名前加.thumb
    """
    # 获取当前目录
    current_dir = os.path.dirname(os.path.abspath(__file__))
    
    # 查找所有子目录
    for root, dirs, files in os.walk(current_dir):
        # 跳过当前目录（只处理子目录）
        if root == current_dir:
            continue
            
        print(f"处理目录: {root}")
        
        # 查找当前目录中的所有webp文件
        webp_files = glob.glob(os.path.join(root, "*.webp"))
        
        for webp_file in webp_files:
            try:
                # 打开图片
                with Image.open(webp_file) as img:
                    # 获取原图尺寸
                    width, height = img.size
                    max_dimension = max(width, height)
                    
                    # 如果最大尺寸不超过1000，则直接复制
                    if max_dimension <= 1000:
                        # 构建新文件名
                        base_name = os.path.basename(webp_file)
                        name_parts = os.path.splitext(base_name)
                        new_filename = f"{name_parts[0]}.thumb{name_parts[1]}"
                        new_filepath = os.path.join(root, new_filename)
                        
                        # 直接保存（不改变尺寸）
                        img.save(new_filepath, 'WEBP', quality=90)
                        print(f"  直接保存: {base_name} -> {new_filename}")
                    
                    else:
                        # 计算新尺寸（保持宽高比）
                        if width > height:
                            new_width = 1000
                            new_height = int(height * 1000 / width)
                        else:
                            new_height = 1000
                            new_width = int(width * 1000 / height)
                        
                        # 调整图片尺寸
                        resized_img = img.resize((new_width, new_height), Image.Resampling.LANCZOS)
                        
                        # 构建新文件名
                        base_name = os.path.basename(webp_file)
                        name_parts = os.path.splitext(base_name)
                        new_filename = f"{name_parts[0]}.thumb{name_parts[1]}"
                        new_filepath = os.path.join(root, new_filename)
                        
                        # 保存压缩后的图片
                        resized_img.save(new_filepath, 'WEBP', quality=90)
                        print(f"  压缩: {base_name} ({width}x{height} -> {new_width}x{new_height}) -> {new_filename}")
                        
            except Exception as e:
                print(f"  处理文件 {webp_file} 时出错: {str(e)}")

if __name__ == "__main__":
    print("开始压缩webp图片...")
    compress_webp_images()
    print("压缩完成！")