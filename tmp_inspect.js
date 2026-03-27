const fs = require('fs');
const path = require('path');

const filePath = path.join(__dirname, 'src', 'main', 'resources', 'static', 'index.html');
const content = fs.readFileSync(filePath, 'utf8');

const sidebarMatch = content.match(/<div class="sidebar" id="sidebar">([\s\S]*?)<\/div>\s*<!-- MAIN CONTENT -->/);
if (sidebarMatch) {
  console.log("SIDEBAR STRUCTURE FOUND:");
  console.log(sidebarMatch[1].substring(0, 1000));
} else {
  console.log("SIDEBAR NOT FOUND");
}
