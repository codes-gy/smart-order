import { cn } from "@/utils/cn";

function stringToHue(input: string): number {
  let hash = 0;
  for (let i = 0; i < input.length; i += 1) {
    hash = input.charCodeAt(i) + ((hash << 5) - hash);
  }
  return Math.abs(hash) % 360;
}

export interface StoreThumbnailProps {
  name: string;
  className?: string;
}

/**
 * 실제 매장 이미지 연동 전 단계의 자리 표시용 썸네일.
 * 매장명 해시로 색상을, "스마트오더 " 접두어를 제외한 첫 글자를 라벨로 사용해
 * 매장별로 시각적으로 구분되도록 한다.
 */
export function StoreThumbnail({ name, className }: StoreThumbnailProps) {
  const branchLabel = name.replace(/^스마트오더\s*/, "");
  const hue = stringToHue(name);

  return (
    <div
      aria-hidden="true"
      className={cn(
        "flex items-center justify-center rounded-xl font-bold text-white",
        className,
      )}
      style={{ backgroundColor: `hsl(${hue} 45% 40%)` }}
    >
      {branchLabel.charAt(0) || name.charAt(0)}
    </div>
  );
}
