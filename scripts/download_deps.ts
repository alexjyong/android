import { resolve } from "jsr:@std/path"

const outputFolderParent = resolve(Deno.cwd(), "app", "src", "main", "assets")

try {
    Deno.statSync(outputFolderParent)
} catch (_) {
    console.error(
        "\x1b[31m" + // red
            "Did you run this script from the correct directory?" +
            "\x1b[0m"
    )
    console.error(
        "Usage: " +
            "\x1b[35m" + // magenta
            "deno run -A scripts/download_deps.ts" +
            "\x1b[0m" +
            " from the " +
            "\x1b[1;31;4m" + // bold red underline
            "root" +
            "\x1b[0m" +
            " directory of the project."
    )
    Deno.exit(1)
}

const outputFolder = resolve(outputFolderParent, "embedded")

// If it exists, delete it
try {
    Deno.removeSync(outputFolder, { recursive: true })
} catch (_) {
    // Ignore, might not exist
}

// Create the output folder
Deno.mkdirSync(outputFolder, { recursive: true })

const deps = [
    {
        file: "katex.min.css",
        url: "https://cdn.jsdelivr.net/npm/katex@0.16.19/dist/katex.min.css",
    },
    {
        file: "katex.min.js",
        url: "https://cdn.jsdelivr.net/npm/katex@0.16.19/dist/katex.min.js",
    },
    {
        file: "micromark.bundle.js",
        url: "https://esm.sh/v135/micromark@4.0.1/es2022/micromark.bundle.mjs",
    },
]

console.log("Will download the following files:")
for (const dep of deps) {
    console.log(`- ${dep.file} from ${dep.url}`)
}
if (!confirm("Continue?")) {
    console.log("Aborted.")
    Deno.exit(0)
}

for (const dep of deps) {
    const response = await fetch(dep.url)
    const data = await response.arrayBuffer()
    const file = resolve(outputFolder, dep.file)
    Deno.writeFileSync(file, new Uint8Array(data))
    console.log(`Downloaded ${dep.file} to ${file}`)
}

console.log("Done.")
