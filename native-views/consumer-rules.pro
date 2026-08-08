# Okio 1.x references this optional JSR-305 annotation in metadata only.
# It is not required for execution, but R8 otherwise reports it as missing.
-dontwarn javax.annotation.Nullable
