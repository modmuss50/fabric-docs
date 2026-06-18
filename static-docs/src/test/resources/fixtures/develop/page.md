---
title: Page
description: A page
authors:
  - tester
resources:
  https://example.com: Example
---

::: warning IMPORTANT
Read this.
:::

<ChoiceComponent :choices="[
  {
    name: 'Linux',
    href: './linux',
  },
]" />

<<< @/reference/latest/src/main/java/com/example/Test.java#sample

@[code lang=java highlight={1} transcludeWith=:::other](@/reference/latest/src/main/java/com/example/Test.java)

<DownloadEntry visualURL="/assets/example.txt" downloadURL="/assets/example.txt">Download</DownloadEntry>
