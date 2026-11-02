//! Open in RustRover (or IntelliJ with the Rust plugin) to check Rust-specific colors:
//! lifetimes, type parameters, macros, mutable bindings, unsafe and cfg-disabled code.

use std::collections::HashMap;

/// A palette entry with a borrowed `name`. See [`Palette::get`].
#[derive(Debug, Clone)]
struct Swatch<'a> {
    name: &'a str,
    hex: u32,
}

trait Describe {
    fn describe(&self) -> String;
}

impl<'a> Describe for Swatch<'a> {
    fn describe(&self) -> String {
        format!("{} = #{:06x}", self.name, self.hex)
    }
}

struct Palette<'a, T: Describe> {
    entries: HashMap<&'a str, T>,
}

impl<'a, T: Describe> Palette<'a, T> {
    fn get(&self, name: &str) -> Option<&T> {
        self.entries.get(name)
    }
}

const ACCENT: u32 = 0xfce566;
static mut REQUESTS: usize = 0;

#[cfg(feature = "never-enabled")]
fn disabled() -> u8 {
    42
}

fn main() {
    let mut entries = HashMap::new();
    entries.insert("accent", Swatch { name: "accent", hex: ACCENT });
    let palette = Palette { entries };

    let mut count = 0;
    for name in ["accent", "missing"] {
        if let Some(swatch) = palette.get(name) {
            println!("{}", swatch.describe());
            count += 1;
        }
    }
    unsafe {
        REQUESTS += count;
    }
    assert_eq!(count, 1, "expected one match for {:?}", 'a');
}
