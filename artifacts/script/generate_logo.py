"""用透明底 WebP 统一生成项目 Logo。依赖 Pillow，可在 Windows 上运行。

用法：python generate_logo.py "D:\\1.webp"
目标路径相对于脚本所在的仓库根目录，不受运行时工作目录影响。
"""

import argparse
import struct
import sys
from dataclasses import dataclass
from io import BytesIO
from pathlib import Path

from PIL import Image, ImageDraw


@dataclass(frozen=True)
class LogoTarget:
    """path 为仓库相对路径；size 为正方形边长，ICO 中表示最大尺寸。"""

    path: str
    size: int
    rounded_background: bool = False


# 配置区：新增位置时添加 LogoTarget；文件格式由路径后缀决定。
PROJECT_ROOT = Path(__file__).resolve().parents[2]
ICO_SIZES = (16, 32, 48, 64, 128, 256)
ROUND_RADIUS_RATIO = 0.22  # 圆角半径占边长的比例，可按桌面图标外观调整。
BACKGROUND_COLOR = (255, 255, 255, 255)
MASK_SCALE = 4  # 超采样圆角蒙版，缩回目标尺寸以获得抗锯齿边缘。
WEBP_QUALITY = 100

LOGO_TARGETS = (
    LogoTarget("ylcs-app/app/global/src/commonMain/composeResources/drawable/img_logo.webp", 256),
    LogoTarget("ylcs-app/android-app/src/main/res/mipmap-xxxhdpi/icon.webp", 512, True),
    LogoTarget("ylcs-app/iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png", 1024, True),
    LogoTarget("ylcs-app/iosApp/iosApp/Assets.xcassets/LaunchScreenLogo.imageset/logo.png", 512, True),
    LogoTarget("ylcs-app/config/icon.png", 256),
    LogoTarget("ylcs-app/config/icon.ico", 256),
    LogoTarget("ylcs-app/config/icon.icns", 256),
    LogoTarget("ylcs-app/web-app/src/webMain/resources/icon.webp", 256, True),
    LogoTarget("ylcs-app/web-app/src/webMain/resources/favicon.ico", 256),
    LogoTarget("ylcs-app/landpage/src/jsMain/resources/favicon.ico", 256),
    LogoTarget("ylcs-app/gallery/src/wasmJsMain/resources/favicon.ico", 256),
    LogoTarget("ylcs-app/modern-screen/src/wasmJsMain/resources/favicon.ico", 256),
    LogoTarget("ylcs-app/server/src/nativeMain/resources/public/emoji/0.webp", 128),
)

# ICNS 中承载单个 PNG 图像的标准块类型，避免默认编码器额外生成 1024 像素图像。
ICNS_TYPES = {
    16: b"icp4",
    32: b"icp5",
    64: b"icp6",
    128: b"ic07",
    256: b"ic08",
    512: b"ic09",
    1024: b"ic10",
}


def load_logo(path: Path) -> Image.Image:
    with Image.open(path) as image:
        if image.format != "WEBP":
            raise ValueError("输入图片必须是 WebP 格式。")
        if getattr(image, "n_frames", 1) != 1:
            raise ValueError("输入图片必须是静态 WebP，不能是动画。")
        if image.width != image.height:
            raise ValueError("输入图片必须是正方形。")
        logo = image.convert("RGBA")

    minimum_alpha, maximum_alpha = logo.getchannel("A").getextrema()
    if minimum_alpha == 255:
        raise ValueError("输入图片必须包含透明背景。")
    if maximum_alpha == 0:
        raise ValueError("输入图片不能完全透明。")
    logo.info.clear()
    return logo


def render_logo(source: Image.Image, size: int, rounded_background: bool) -> Image.Image:
    # 始终缩放完整画布，不裁剪透明边距，也不额外缩小 Logo。
    logo = source.resize((size, size), Image.Resampling.LANCZOS)
    if not rounded_background:
        return logo

    mask_size = size * MASK_SCALE
    mask = Image.new("L", (mask_size, mask_size), 0)
    ImageDraw.Draw(mask).rounded_rectangle(
        (0, 0, mask_size - 1, mask_size - 1),
        radius=mask_size * ROUND_RADIUS_RATIO,
        fill=255,
    )
    mask = mask.resize((size, size), Image.Resampling.LANCZOS)

    # 白色背景占满目标画布，四条边的中点贴边，圆角以外透明。
    background = Image.new("RGBA", (size, size), BACKGROUND_COLOR)
    background.putalpha(mask)
    background.alpha_composite(logo)
    return background


def encode_png(image: Image.Image) -> bytes:
    output = BytesIO()
    image.save(output, format="PNG", optimize=True, compress_level=9)
    return output.getvalue()


def encode_icns(image: Image.Image) -> bytes:
    """直接封装 PNG，无需 macOS、iconutil 或外部转码程序。"""
    icon_type = ICNS_TYPES.get(image.width)
    if icon_type is None:
        raise ValueError(f"ICNS 不支持配置的边长：{image.width}。")
    png = encode_png(image)
    chunk = struct.pack(">4sI", icon_type, len(png) + 8) + png
    return struct.pack(">4sI", b"icns", len(chunk) + 8) + chunk


def encode_logo(source: Image.Image, target: LogoTarget) -> bytes:
    if target.size <= 0:
        raise ValueError(f"目标尺寸必须为正数：{target.path}")
    image = render_logo(source, target.size, target.rounded_background)
    suffix = Path(target.path).suffix.lower()
    if suffix == ".png":
        return encode_png(image)
    if suffix == ".icns":
        return encode_icns(image)

    output = BytesIO()
    if suffix == ".webp":
        image.save(
            output,
            format="WEBP",
            quality=WEBP_QUALITY,
            lossless=False,
            alpha_quality=100,
            method=6,
        )
    elif suffix == ".ico":
        if target.size != max(ICO_SIZES):
            raise ValueError(f"ICO 的 size 应为 {max(ICO_SIZES)}：{target.path}")
        # 各档直接从原始高清画布生成，避免先缩到 256 再连续缩小。
        frames = [
            render_logo(source, size, target.rounded_background)
            for size in ICO_SIZES
            if size != target.size
        ]
        image.save(
            output,
            format="ICO",
            sizes=[(size, size) for size in ICO_SIZES],
            append_images=frames,
        )
    else:
        raise ValueError(f"不支持的目标格式：{target.path}")
    return output.getvalue()


def main() -> int:
    parser = argparse.ArgumentParser(description="将透明底 WebP 生成为项目各位置的 Logo，并直接覆盖原文件。")
    parser.add_argument("logo", type=Path, help="正方形、透明背景的高清 WebP 图片路径")
    args = parser.parse_args()

    try:
        source = load_logo(args.logo.expanduser().resolve())
        # 全部转码完成后再覆盖文件，避免编码失败时留下部分更新的图片。
        outputs = [(target, encode_logo(source, target)) for target in LOGO_TARGETS]
        for index, (target, data) in enumerate(outputs, 1):
            destination = PROJECT_ROOT / target.path
            destination.parent.mkdir(parents=True, exist_ok=True)
            destination.write_bytes(data)
            print(f"[{index}/{len(outputs)}] {destination}")
    except (OSError, ValueError) as error:
        print(f"生成失败：{error}", file=sys.stderr)
        return 1

    print(f"已生成并替换 {len(outputs)} 个 Logo 文件。")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
