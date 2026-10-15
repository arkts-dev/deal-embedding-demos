# Android tests

Discovery boundary tests use injected inference and real protocol admission; live Nano checks require explicit opt-in and retain semantic failures separately from parser/availability errors. Calendar confirmation checks use a controlled dedicated-demo event and delete it afterward. Keep live expectations out of model input.

Prefer short regressions through real compiled artifacts and framework adapters. Keep deterministic model fixtures distinct from live inference evidence. Use controlled test/demo records only, preserve personal records and cleanup issued resources. Do not add source-pattern assertions as substitutes for functional session checks. Guidance example regressions consume the exact framework-authored source pair, with fixture-only host/sample and explicit success/failure/retry; never expose that capability to ordinary apps.
