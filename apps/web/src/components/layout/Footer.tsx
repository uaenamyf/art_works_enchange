export function Footer() {
  return (
    <footer className="border-t border-ink-300 bg-white">
      <div className="mx-auto flex w-full max-w-gallery flex-col gap-2 px-6 py-8 text-sm text-ink-500 md:flex-row md:items-center md:justify-between">
        <span>© {new Date().getFullYear()} CreatorHub. All rights reserved.</span>
        <span>独立创作者作品交易社区</span>
      </div>
    </footer>
  );
}
