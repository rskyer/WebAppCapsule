let show = document.getElementById("show")

let formol = ""
let formolOrg = "0"

// 0-9 :
let zero = document.getElementById("zero")
let one = document.getElementById("one")
let two = document.getElementById("two")
let three = document.getElementById("three")
let four = document.getElementById("four")
let five = document.getElementById("five")
let six = document.getElementById("six")
let seven = document.getElementById("seven")
let eight = document.getElementById("eight")
let nine = document.getElementById("nine")

// adds ADD:
let removeLastBtn = document.getElementById("removeLast")
let removeBtn = document.getElementById("C")
let addAddBtn = document.getElementById("addAdd")
let addTaghLBtn = document.getElementById("addTaghL")
let addTaghBtn = document.getElementById("addTagh")
let addZarbBtn = document.getElementById("addZarb")
let addNegBtn = document.getElementById("addNeg")
let addPlusBtn = document.getElementById("addPlus")
let addDotBtn = document.getElementById("addDot")
let addPowerBtn = document.getElementById("addPower")
let getMachieBtn = document.getElementById("getMachine")


function add(harf) {
    formol = formol + harf
    show.textContent = formol
}
function addT(numy) {
    formolOrg = formolOrg + numy
}
function clickedOne() {
 
        add("1")
        addT("1")
}
function clickedZero() {
    add("0")
    addT("0")
}


function clickedTwo() {
    add("2")
    addT("2")
}

function clickedThree() {
    add("3")
    addT("3")
}

function clickedFour() {
    add("4")
    addT("4")
}

function clickedFive() {
    add("5")
    addT("5")
}

function clickedSix() {
    add("6")
    addT("6")
}

function clickedSeven() {
    add("7")
    addT("7")
}

function clickedEight() {
    add("8")
    addT("8")
}

function clickedNine() {
    add("9")
    addT("9")
}
function remove() {
    formol = ""
    formolOrg = "0"
    show.textContent = "0"
}
function removeLast() {
    if (formol.length > 1) {
        formol = formol.slice(0, -1);
        formolOrg = formolOrg.slice(0, -1);
    } else {
        formol = ""
        
        
        formolOrg = "0";
    }
    show.textContent = formol;
}
function addDot() {
    if(formol[formol.length - 1] === "." || formol.endsWith("+") || formol.endsWith("×") || formol.endsWith("÷") || formol.endsWith("-") || formol.endsWith("*") || formol.endsWith(".")) {
        alert("ممکن است مشکلی به وجود بیاید")
    } else {
        add(".")
        addT(".")
    }
}
function addPlus() {
    
       if(formol.endsWith("+") || formol.endsWith("×") || formol.endsWith("÷") || formol.endsWith("-") || formol.endsWith("*") || formol.endsWith(".")) {
           alert("همچین چیزی در ریاضیات ممکن نیست!")
       } else {
           add("+")
           addT("+")
       }
    
}
function addNeg() {
    
       if(formol.endsWith("+") || formol.endsWith("×") || formol.endsWith("÷") || formol.endsWith("-") || formol.endsWith("*") || formol.endsWith(".")) {
           alert("همچین چیزی در ریاضیات ممکن نیست!")
       } else {
           add("-")
           addT("-")
       }
    
}
function addZarb() {
    
       if(formol.endsWith("+") || formol.endsWith("×") || formol.endsWith("÷") || formol.endsWith("-") || formol.endsWith("*") || formol.endsWith(".")) {
           alert("همچین چیزی در ریاضیات ممکن نیست!")
       } else {
           add("×")
           addT("*")
       }
    
}
function addTagh() {
    
       if(formol.endsWith("+") || formol.endsWith("×") || formol.endsWith("÷") || formol.endsWith("-") || formol.endsWith("*") || formol.endsWith(".")) {
           alert("همچین چیزی در ریاضیات ممکن نیست!")
       } else {
           add("÷")
           addT("/")
       }
    
}
function addTaghL() {
    
       if(formol.endsWith("+") || formol.endsWith("×") || formol.endsWith("÷") || formol.endsWith("-") || formol.endsWith("*") || formol.endsWith(".")) {
           alert("همچین چیزی در ریاضیات ممکن نیست!")
       } else {
           add("%")
           addT("%")
       }
    
}
function addPower() {
    
       if(formol.endsWith("+") || formol.endsWith("×") || formol.endsWith("÷") || formol.endsWith("-") || formol.endsWith("*") || formol.endsWith(".")) {
           alert("همچین چیزی در ریاضیات ممکن نیست!")
       } else {
           add("*")
           addT("**")
       }
    
}
function addAdd() {
    if(formol.includes("(")) {
        add(")")
        addT(")")
    } else {
        add("(")
        addT("(")
    }
}


function pus() {
    try {
        let expr = formolOrg
            .replace(/\s/g, "")
            .replace(/×/g, "*")
            .replace(/÷/g, "/")
        
            .replace(/(\d)\(/g, '(')   // 5(3) → 5*(3)
            .replace(/\)\(/g, ')(')      // )( → )*(
            .replace(/\(\)/g, '');        // حذف پرانتز خالی

        // حذف صفرهای اضافی ابتدای اعداد (مثل 08 → 8) اما 0 تنها را نگه دار
        expr = expr.replace(/\b0+(\d+)/g, '$1');

        // اگر عبارت خالی یا فقط پرانتز است، مقدار 0 بگذار
        if (!expr || expr === "" || expr === "()" || expr === "(" || expr === ")") {
            show.textContent = "0";
            formol = "0";
            formolOrg = "0";
            return;
        }

        // برای دیباگ – در کنسول ببینید عبارت چیست
        console.log("عبارت برای محاسبه:", expr);

        let result = Function('return (' + expr + ')')();

        // گرد کردن اعداد اعشاری
        if (typeof result === "number" && !Number.isInteger(result)) {
            result = parseFloat(result.toPrecision(12));
        }

        show.textContent = result;
        formol = String(result);
        formolOrg = String(result);

    } catch (error) {
        show.textContent = "خطا!";
        console.error("عبارت:", formolOrg);
        console.error("خطا:", error.message);
    }
}