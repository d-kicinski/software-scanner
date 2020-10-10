#pragma once

#include <string>

namespace scanner {

void draw_contour(unsigned long int input_ptr);

void software_scanner(unsigned long int input_ptr, unsigned long int output_ptr);

auto hello() -> std::string;

} // namespace scanner
