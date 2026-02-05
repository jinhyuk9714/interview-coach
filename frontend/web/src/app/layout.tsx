import type { Metadata } from "next";
import "./globals.css";
import { Providers } from "@/components/providers";

export const metadata: Metadata = {
  title: "Interview Coach | AI 면접 코칭",
  description: "JD 기반 맞춤 질문 생성 + AI 모의 면접 + 실시간 피드백 시스템",
  keywords: ["면접", "AI", "코칭", "취업", "모의면접"],
};

export default function RootLayout({
  children,
}: Readonly<{
  children: React.ReactNode;
}>) {
  return (
    <html lang="ko">
      <body className="min-h-screen">
        <Providers>
          <div className="noise-overlay" />
          {children}
        </Providers>
      </body>
    </html>
  );
}
