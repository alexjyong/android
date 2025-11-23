---
title: Why does Stoat for Android bundle CA certificates?
description: Explanation of the bundled CA certificates in Stoat for Android.
template: doc
---

Stoat for Android comes with a set of bundled CA (Certificate Authority) certificates.

This is mostly due to the fact that Cloudflare — one of the major CDN providers used by Stoat —
issues CA certificates which are not included in Android's system CA store. By bundling our own CA
certificates, we ensure that Stoat for Android can always connect to Stoat's servers regardless.

This should not have security implications, as the bundled CA certificates are only trusted when
connecting to the domain `stoatusercontent.com` and subdomains thereof. For all other connections,
Stoat uses the system CA store provided by Android.

## What certificates are bundled?

The bundled CA certificates are the following:

- [SSL.com TLS ECC Root CA 2022](https://crt.sh/?id=7439766705)
- [Cloudflare TLS Issuing ECC CA 1](https://crt.sh/?id=11092622664)

## Why not use the system CA store?

Google rejects those certificates from their system CA store due to unclear reasons. As a result, if
we were to use the system CA store, Stoat for Android would not be able to connect to Stoat's
content delivery network (CDN) at all, rendering the app bare as it would not be able to load any
images or other media.