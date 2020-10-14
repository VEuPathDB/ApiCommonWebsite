# ApiCommonWebsite
Frontend pages (js, css, images, html) common to all VEuPathDB genomic sites. Api stands for 'Apicomplexan'. These are the first organisms our websites on their first public release as ApiDB BRC (Bioinformatics Resource Center), back in 2002.

Our frontend code stored in this repo has evolved in the last few years from a Struts based framework (jsx pages) to a modern REST-based architecture with a javascript/typescript React client as frontend. We plan to rename eventually this project as ApiClientCommon.

## Description

`ApiCommonWebsite` contains an extension of the [WDKClient](https://github.com/VEuPathDB/WDKClient) for our VEuPathDB sites.
As with the `WDKClient`, `ApiCommonWebsite` React-based client code is mostly written in [TypeScript](https://www.typescriptlang.org/) and SCSS
([Sass](https://sass-lang.com/)).


## Installation and Usage

Presently, `ApiCommonWebsite` should be installed following the [Strategies WDK
Documentation](https://docs.google.com/document/u/1/d/1nZayjR-0Hj3YeukjfwoWZ3TzokuuuWvSwnhw_q41oeE/pub).

Dependencies are managed with [yarn](https://yarnpkg.com/).

Tests are written for the [jest](https://jestjs.io/) testing framework.
