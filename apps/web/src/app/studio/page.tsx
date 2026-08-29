import { redirect } from "next/navigation";

/** Studio 默认入口直接进入当前已提供的作品管理工作流。 */
export default function StudioPage() {
  redirect("/studio/artworks");
}