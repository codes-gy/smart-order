import { Badge } from "@/components/ui/Badge";
import type { RewardsSummary } from "@/types/auth.types";

/** 마이페이지 등에서 보유 스탬프/쿠폰 현황을 보여주는 뱃지 그룹 */
export function RewardsBadge({ rewards }: { rewards: RewardsSummary }) {
  return (
    <div className="flex flex-wrap items-center gap-2">
      <Badge variant="accent">
        스탬프 {rewards.stampCount}/{rewards.stampGoal}
      </Badge>
      {rewards.availableCouponCount > 0 && (
        <Badge variant="success">사용 가능 쿠폰 {rewards.availableCouponCount}장</Badge>
      )}
    </div>
  );
}
