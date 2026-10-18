import NextAuth from "next-auth";
import Google from "next-auth/providers/google";

type KcmartCustomer = {
  id: number;
  email: string;
  name: string | null;
  imageUrl: string | null;
  active: boolean;
  createdAt: string;
};

async function synchronizeKcmartCustomer(input: {
  provider: string;
  providerSubject: string;
  email: string;
  name: string | null;
  imageUrl: string | null;
}): Promise<KcmartCustomer> {
  const backendUrl =
    process.env.KCMART_API_URL?.replace(/\/+$/, "");

  const internalKey =
    process.env.KCMART_INTERNAL_API_KEY;

  if (!backendUrl) {
    throw new Error(
      "KCMART_API_URL is not configured."
    );
  }

  if (!internalKey) {
    throw new Error(
      "KCMART_INTERNAL_API_KEY is not configured."
    );
  }

  const response = await fetch(
    `${backendUrl}/api/v1/internal/customers/sync`,
    {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-KCMART-Internal-Key": internalKey,
      },
      body: JSON.stringify(input),
      cache: "no-store",
    }
  );

  if (!response.ok) {
    throw new Error(
      `KCMART customer synchronization failed with HTTP ${response.status}.`
    );
  }

  const customer =
    (await response.json()) as KcmartCustomer;

  if (
    typeof customer.id !== "number" ||
    !customer.email ||
    customer.active !== true
  ) {
    throw new Error(
      "KCMART backend returned an invalid customer."
    );
  }

  return customer;
}

export const { handlers, auth, signIn, signOut } =
  NextAuth({
    providers: [
      Google({
        clientId: process.env.AUTH_GOOGLE_ID,
        clientSecret:
          process.env.AUTH_GOOGLE_SECRET,
      }),
    ],

    session: {
      strategy: "jwt",
    },

    pages: {
      signIn: "/account",
    },

    callbacks: {
      async jwt({
        token,
        account,
        profile,
      }) {
        /*
         * account + profile are available during the
         * initial Google OAuth sign-in.
         *
         * Google "sub" is the stable Google account ID.
         * It stays inside the Auth.js JWT and is NOT
         * copied into the browser session.
         */
        if (
          account?.provider === "google" &&
          typeof profile?.sub === "string"
        ) {
          if (
            typeof profile.email !== "string" ||
            !profile.email
          ) {
            throw new Error(
              "Google did not provide an email address."
            );
          }

          /*
           * Google normally provides email_verified.
           * If the field is explicitly false, reject
           * synchronization.
           */
          if (
            "email_verified" in profile &&
            profile.email_verified === false
          ) {
            throw new Error(
              "Google email is not verified."
            );
          }

          const customer =
            await synchronizeKcmartCustomer({
              provider: "google",
              providerSubject: profile.sub,
              email: profile.email,
              name:
                typeof profile.name === "string"
                  ? profile.name
                  : null,
              imageUrl:
                typeof profile.picture === "string"
                  ? profile.picture
                  : null,
            });

          token.kcmartProvider = "google";
          token.kcmartProviderSubject =
            profile.sub;

          /*
           * Customer ID also remains server-side in
           * the Auth.js JWT for future checkout work.
           *
           * We deliberately do not add a session()
           * callback exposing these fields.
           */
          token.kcmartCustomerId =
            customer.id;
        }

        return token;
      },
    },
  });