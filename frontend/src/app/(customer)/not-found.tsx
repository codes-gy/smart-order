import Link from "next/link";
import { Button } from "@/components/ui/Button";

export default function CustomerNotFound() {
  return (
    <div className="flex min-h-screen flex-col items-center justify-center gap-4 p-6 text-center">
      <p className="text-lg font-semibold text-foreground">페이지를 찾을 수 없어요</p>
      <p className="text-sm text-foreground/60">주소를 다시 확인하거나 홈으로 돌아가주세요.</p>
      <Link href="/">
        <Button>홈으로 가기</Button>
      </Link>
    </div>
  );
}
