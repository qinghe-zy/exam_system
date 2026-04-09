from pathlib import Path

from PIL import Image, ImageOps


ROOT = Path(__file__).resolve().parent.parent
OUTPUT_DIR = ROOT / "docs" / "assets" / "readme"
OUTPUT_DIR.mkdir(parents=True, exist_ok=True)

CANVAS_SIZE = (1600, 1200)  # 4:3
BACKGROUND = (246, 243, 237)
CARD_BACKGROUND = (255, 255, 255)
CARD_PADDING = 48

IMAGE_MAP = {
    "architecture-4x3.png": ROOT / "docs" / "assets" / "diagrams" / "system-architecture.png",
    "business-flow-4x3.png": ROOT / "docs" / "assets" / "diagrams" / "core-business-flow.png",
    "deployment-topology-4x3.png": ROOT / "docs" / "assets" / "diagrams" / "deployment-topology.png",
    "login-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "01-login.png",
    "dashboard-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "02-dashboard.png",
    "question-bank-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "05-question-bank.png",
    "paper-builder-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "06-paper-builder.png",
    "exam-plans-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "07-exam-plans.png",
    "student-exam-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "10-student-exam.png",
    "grading-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "08-grading.png",
    "analytics-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "09-analytics.png",
    "role-permissions-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "03-role-permissions.png",
    "system-config-4x3.png": ROOT / "docs" / "assets" / "screenshots" / "04-system-config.png",
}


def build_canvas(source_path: Path, target_path: Path) -> None:
    with Image.open(source_path) as original:
        image = original.convert("RGB")
        canvas = Image.new("RGB", CANVAS_SIZE, BACKGROUND)

        inner_width = CANVAS_SIZE[0] - CARD_PADDING * 2
        inner_height = CANVAS_SIZE[1] - CARD_PADDING * 2
        fitted = ImageOps.contain(image, (inner_width, inner_height), method=Image.Resampling.LANCZOS)

        card_width = fitted.width + CARD_PADDING
        card_height = fitted.height + CARD_PADDING
        card_left = (CANVAS_SIZE[0] - card_width) // 2
        card_top = (CANVAS_SIZE[1] - card_height) // 2

        card = Image.new("RGB", (card_width, card_height), CARD_BACKGROUND)
        canvas.paste(card, (card_left, card_top))

        image_left = (CANVAS_SIZE[0] - fitted.width) // 2
        image_top = (CANVAS_SIZE[1] - fitted.height) // 2
        canvas.paste(fitted, (image_left, image_top))
        canvas.save(target_path, quality=95)


def main() -> None:
    for output_name, source_path in IMAGE_MAP.items():
        build_canvas(source_path, OUTPUT_DIR / output_name)
    print(f"Generated {len(IMAGE_MAP)} README images in {OUTPUT_DIR}")


if __name__ == "__main__":
    main()
