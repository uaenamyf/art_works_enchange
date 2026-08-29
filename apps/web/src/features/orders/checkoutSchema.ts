import { z } from "zod";

/** 结算表单值；产品类型由服务端作品快照决定，不能由用户修改。 */
export const checkoutSchema = z
  .object({
    quantity: z.coerce.number().int().min(1, "至少购买 1 件").max(99, "最多购买 99 件"),
    productType: z.enum(["PHYSICAL", "DIGITAL"]).default("PHYSICAL"),
    recipientName: z.string().max(100).optional(),
    line1: z.string().max(200).optional(),
    city: z.string().max(100).optional(),
    postalCode: z.string().max(20).optional(),
    countryCode: z.string().optional(),
    phone: z.string().max(30).optional()
  })
  .superRefine((values, context) => {
    if (values.productType === "DIGITAL") return;
    const requiredFields: Array<[keyof CheckoutValues, string]> = [
      ["recipientName", "请输入收货人"],
      ["line1", "请输入地址"],
      ["city", "请输入城市"],
      ["postalCode", "请输入邮编"],
      ["phone", "请输入有效手机号"]
    ];
    for (const [field, message] of requiredFields) {
      if (!values[field]) {
        context.addIssue({ code: z.ZodIssueCode.custom, path: [field], message });
      }
    }
    if (!values.countryCode || !/^[A-Z]{2}$/.test(values.countryCode)) {
      context.addIssue({
        code: z.ZodIssueCode.custom,
        path: ["countryCode"],
        message: "请输入两位国家代码"
      });
    }
  });

export type CheckoutValues = z.infer<typeof checkoutSchema>;