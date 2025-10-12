import { defineConfig } from "astro/config"
import starlight from "@astrojs/starlight"
import markdownIntegration from "@astropub/md"

// https://astro.build/config
export default defineConfig({
    site: "https://stoatchat.github.io",
    base: "/for-android",
    integrations: [
        starlight({
            title: "Stoat for Android Technical Documentation",
            social: {
                github: "https://github.com/stoatchat/for-android",
            },
            sidebar: [
                {
                    label: "Contributing",
                    items: [
                        // Each item here is one entry in the navigation menu.
                        {
                            label: "Guidelines",
                            link: "/contributing/guidelines",
                        },
                        {
                            label: "Setup",
                            link: "/contributing/setup",
                        }
                    ],
                }
                /* {
                    label: "Reference",
                    autogenerate: { directory: "reference" },
                }, */
            ],
            customCss: ["./src/styles/custom.css"],
        }),
        markdownIntegration(),
    ],
})
